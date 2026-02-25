#!/usr/bin/env node

/**
 * Generate test JWT tokens for Bruno integration tests.
 * These tokens contain permission claims required by the authorization policies.
 * Tokens are HMAC-SHA256 signed with a well-known test secret.
 * 
 * Usage: node generate-test-tokens.js
 * Output: Displays tokens to copy into Bruno environment
 * 
 * IMPORTANT: The secret key must match the one in petstore-tests/PetstoreApi/Program.cs
 */

const crypto = require('crypto');

// Well-known test secret (must match Program.cs) - min 256 bits for HS256
const TEST_SECRET = 'this-is-a-test-secret-key-for-petstore-api-dev-only-min-32-bytes!';

function base64UrlEncode(data) {
    const b64 = (typeof data === 'string') 
        ? Buffer.from(data).toString('base64')
        : data.toString('base64');
    return b64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
}

function createToken(claims) {
    const header = {
        alg: 'HS256',
        typ: 'JWT'
    };
    
    const payload = {
        sub: 'test-user-' + claims.permission,
        name: 'Test User',
        iat: Math.floor(Date.now() / 1000),
        exp: Math.floor(Date.now() / 1000) + (365 * 24 * 60 * 60), // 1 year
        ...claims
    };
    
    const encodedHeader = base64UrlEncode(JSON.stringify(header));
    const encodedPayload = base64UrlEncode(JSON.stringify(payload));
    
    // Sign with HMAC-SHA256
    const signature = crypto.createHmac('sha256', TEST_SECRET)
        .update(`${encodedHeader}.${encodedPayload}`)
        .digest();
    const encodedSignature = base64UrlEncode(signature);
    
    return `${encodedHeader}.${encodedPayload}.${encodedSignature}`;
}

console.log('\n=== Test JWT Tokens for Bruno Integration Tests ===\n');
console.log('Copy these tokens into bruno/OpenAPI_Petstore/environments/local.bru\n');

const readToken = createToken({ permission: 'read' });
const writeToken = createToken({ permission: 'write' });
const readWriteToken = createToken({ permission: 'read,write' });

console.log('Read Token (permission: read):');
console.log(readToken);
console.log('\n');

console.log('Write Token (permission: write):');
console.log(writeToken);
console.log('\n');

console.log('Read+Write Token (permission: read,write):');
console.log(readWriteToken);
console.log('\n');

console.log('=== Bruno Environment Configuration ===\n');
console.log('Add these variables to bruno/OpenAPI_Petstore/environments/local.bru:\n');
console.log('vars {');
console.log('  baseUrl: http://localhost:5198/v2');
console.log('  petId: 12345');
console.log(`  readToken: ${readToken}`);
console.log(`  writeToken: ${writeToken}`);
console.log(`  readWriteToken: ${readWriteToken}`);
console.log('  authTestPetId: 99991');
console.log('}\n');
