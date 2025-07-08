import hashlib
import secrets

access_key = secrets.token_urlsafe(32)
new_hash = hashlib.sha256(access_key.encode()).hexdigest()
print(access_key)
print(new_hash)
