const fs = require('fs');
const path = require('path');
const dir = 'c:\\\\Users\\\\nandh\\\\AndroidStudioProjects\\\\KhanaBook\\\\server\\\\src\\\\main\\\\java\\\\com\\\\khanabook\\\\saas\\\\entity';

fs.readdirSync(dir).forEach(file => {
    if(!file.endsWith('.java')) return;
    let p = path.join(dir, file);
    let content = fs.readFileSync(p, 'utf8');
    
    // Add Index import if missing
    if(!content.includes('import jakarta.persistence.Index;')) {
        content = content.replace('import jakarta.persistence.Table;', 'import jakarta.persistence.Table;\nimport jakarta.persistence.Index;');
    }
    
    // Process regular @Table(...) annotations
    let m = content.match(/@Table\(name\s*=\s*"([^"]+)"\)/);
    if(m && !content.includes('indexes = {') && file !== 'User.java') {
        let tableName = m[1];
        let rep = '@Table(name = "' + tableName + '", indexes = {\n    @Index(name = "idx_' + tableName + '_tenant_updated", columnList = "restaurant_id, updated_at"),\n    @Index(name = "idx_' + tableName + '_device", columnList = "restaurant_id, device_id, local_id")\n})';
        content = content.replace(m[0], rep);
        fs.writeFileSync(p, content);
        console.log('Updated ' + file);
    }
    
    // Process User.java separately 
    if (file === 'User.java' && m && !content.includes('indexes = {')) {
        let tableName = m[1];
        let rep = '@Table(name = "' + tableName + '", indexes = {\n    @Index(name = "idx_' + tableName + '_tenant_updated", columnList = "restaurant_id, updated_at"),\n    @Index(name = "idx_' + tableName + '_device", columnList = "restaurant_id, device_id, local_id"),\n    @Index(name = "idx_' + tableName + '_email", columnList = "email", unique = true)\n})';
        content = content.replace(m[0], rep);
        fs.writeFileSync(p, content);
        console.log('Updated ' + file);
    }
});
