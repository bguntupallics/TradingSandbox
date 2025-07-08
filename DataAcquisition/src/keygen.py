import secrets

# 16 bytes → 32 hex characters
access_key = secrets.token_hex(16)
print(access_key)  # e.g. '9f2c4a1d8e5b3f7a4c6d9e0b2f1a8c3d'
