# JmeAssetImporter
A tool to automatically import assets from blender to jME, including copying modified textures.

### Asset import description file

```
global {
    blendpath: [ "/path/to/blend/files" "/another/path"]
    assetpath: "/workspace/jme project/assets"
    out-path: /Models
    out-texture-path: /Textures
    copy-texture: true
}
flowers {
    in-blend: blendfile.blend 
    out-j3o: [ 
               ["Blue Flower" "flower1.j3o" ] 
               ["Pink Flower" "flower2.j3o"] 
               ["Yellow Flower" "flower3.j3o"] 
             ]
}
trees {
    in-blend: blendfile2.blend 
    out-j3o: [ 
               ["Willow" "tree1.j3o" ]
               ["Oak" "tree2.j3o"]
             ]
    filter: myfilter.ExampleImportFilter
}
```
The first global block, specifies default properties. The other block can overwrite these if needed. 
Objects are imported from blender files bases on the Object name in blender.

### Running the tool
Run the main class AssetImporter with arguments.

The first argument is a path to asset import file. 
Optionally, more arguments specify which blocks of the file to import. Not specifying any will import all by default.

### Dependencies
jME core libraries, jme3-blender.jar

