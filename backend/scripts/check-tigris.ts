/**
 * Tigris/S3 connectivity check.
 *
 * Reads S3_* from the environment, falling back to backend/.env, then PUTs a
 * probe object into the configured bucket to validate endpoint, credentials,
 * region and bucket name. Prints the result or the full AWS error.
 *
 * Usage (from backend/):
 *   npx tsx scripts/check-tigris.ts
 */
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { DeleteObjectCommand, PutObjectCommand, S3Client } from '@aws-sdk/client-s3';

function loadEnvFile(): Record<string, string> {
    const result: Record<string, string> = {};

    try {
        const raw = readFileSync(join(process.cwd(), '.env'), 'utf8');

        for (const line of raw.split('\n')) {
            const trimmed = line.trim();

            if (!trimmed || trimmed.startsWith('#') || !trimmed.includes('=')) {
                continue;
            }

            const [key, ...rest] = trimmed.split('=');

            const value = rest.join('=').replace(/^["']|["']$/g, '');

            result[key] = value;
        }
    } catch {
        // no .env present — rely on process.env only
    }

    return result;
}

function get(envFile: Record<string, string>, key: string): string | undefined {
    return process.env[key] ?? envFile[key];
}

async function main() {
    const envFile = loadEnvFile();

    const bucket = get(envFile, 'S3_BUCKET');
    const endpoint = get(envFile, 'S3_ENDPOINT');
    const region = get(envFile, 'S3_REGION') ?? 'auto';
    const accessKeyId = get(envFile, 'S3_ACCESS_KEY_ID');
    const secretAccessKey = get(envFile, 'S3_SECRET_ACCESS_KEY');
    const forcePathStyle = (get(envFile, 'S3_FORCE_PATH_STYLE') ?? 'true').toLowerCase() === 'true';

    const prefix = (get(envFile, 'S3_PREFIX') ?? '').replace(/^\/+|\/+$/g, '');

    const required: Array<[string, string | undefined]> = [
        ['S3_BUCKET', bucket],
        ['S3_ENDPOINT', endpoint],
        ['S3_ACCESS_KEY_ID', accessKeyId],
        ['S3_SECRET_ACCESS_KEY', secretAccessKey],
    ];

    const missing = required.filter(([, v]) => !v).map(([k]) => k);

    if (missing.length > 0) {
        console.error(`Missing S3 config: ${missing.join(', ')}`);
        console.error('Set them in your environment or backend/.env.');
        process.exit(2);
    }

    console.log('Endpoint:', endpoint);
    console.log('Region:  ', region);
    console.log('Bucket:  ', bucket);
    console.log('PathStyle:', forcePathStyle);
    console.log('AccessKey:', accessKeyId ? `${accessKeyId.slice(0, 6)}...` : '');

    const client = new S3Client({
        region,
        endpoint,
        forcePathStyle,
        credentials: { accessKeyId: accessKeyId!, secretAccessKey: secretAccessKey! },
    });

    const key = prefix ? `${prefix}/__r2r_probe__` : `__r2r_probe__`;

    console.log('Key name:', key);

    try {
        await client.send(
            new PutObjectCommand({
                Bucket: bucket,
                Key: key,
                Body: Buffer.from('probe'),
            }),
        );

        console.log('PUT OK — bucket, endpoint, region and credentials are all valid.');

        await client.send(
            new DeleteObjectCommand({
                Bucket: bucket,
                Key: key,
            }),
        );

        console.log('Probe object removed.');
        process.exit(0);
    } catch (error) {
        const name = (error as { name?: string })?.name ?? 'S3Error';
        const status = (error as { $metadata?: { httpStatusCode?: number } })?.$metadata?.httpStatusCode;
        const message = (error as Error)?.message;

        console.error(`\nWRITE FAILED (${name}${status ? `, HTTP ${status}` : ''})`);
        console.error(message);

        console.error('\n--- Diagnostics: what can this key do? ---');

        const { ListObjectsV2Command } = await import('@aws-sdk/client-s3');

        for (const listPrefix of ['', prefix]) {
            try {
                const listed = await client.send(
                    new ListObjectsV2Command({
                        Bucket: bucket,
                        Prefix: listPrefix,
                        MaxKeys: 3,
                    }),
                );

                const keys = (listed.Contents ?? []).map((o) => o.Key);

                console.log(`LIST bucket="${bucket}" prefix="${listPrefix}" -> OK (${keys.length} objects: ${keys.join(', ') || 'none'})`);
            } catch (listError) {
                const listName = (listError as { name?: string })?.name ?? 'S3Error';
                const listStatus = (listError as { $metadata?: { httpStatusCode?: number } })?.$metadata?.httpStatusCode;

                console.log(`LIST bucket="${bucket}" prefix="${listPrefix}" -> ${listName}${listStatus ? ` (HTTP ${listStatus})` : ''}`);
            }
        }

        console.error('\nHint: the key is valid (403, not InvalidAccessKeyId) but its Tigris policy\n' +
            'allows a different bucket/prefix, or omits `put`. Check the key in the Tigris\n' +
            'console: Bucket, Prefix and Actions (should include put/delete/get/list).');

        process.exit(1);
    }
}

void main();