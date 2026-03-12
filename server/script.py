import os
import glob
import re

directory = r'c:\Users\nandh\AndroidStudioProjects\KhanaBook\server\src\main\java\com\khanabook\saas\entity'

for filepath in glob.glob(os.path.join(directory, '*.java')):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Add Index import if missing
    if 'import jakarta.persistence.Index;' not in content and 'import jakarta.persistence.Table;' in content:
        content = content.replace('import jakarta.persistence.Table;', 'import jakarta.persistence.Table;\nimport jakarta.persistence.Index;')
    
    # Replace @Table with indexes
    table_match = re.search(r'@Table\(name\s*=\s*\"([^\"]+)\"\)', content)
    if table_match:
        table_name = table_match.group(1)
        replacement = f'@Table(name = "{table_name}", indexes = {{\n    @Index(name = "idx_{table_name}_tenant_updated", columnList = "restaurant_id, updated_at"),\n    @Index(name = "idx_{table_name}_device", columnList = "restaurant_id, device_id, local_id")\n}})'
        # make sure it doesn't already have indexes
        if 'indexes = {' not in content:
            content = content.replace(table_match.group(0), replacement)
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f'Updated {filepath}')
