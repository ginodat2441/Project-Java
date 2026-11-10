const fs=require('fs'),path=require('path');
const root=path.resolve(__dirname,'../../backend_java/src/main/resources/static');
const manifest=require(path.join(root,'admin/js/manifest.json'));
exports.readAsset=name=>(manifest[name]||[name]).map(p=>fs.readFileSync(path.join(root,p),'utf8')).join('\n');
