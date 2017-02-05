/*
 * Copyright (c) 2017, Juraj Papp
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the copyright holder nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme.blend.data.io;

import com.jme3.asset.AssetManager;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.blender.textures.TextureHelper;
import com.jme3.system.JmeSystem;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme.blend.data.io.text.PropertyReader;
import jme.blend.data.io.text.PropertyReader.PropertyParser;
import jme.blend.data.io.blend.*;

/**
 *
 * @author Juraj Papp
 */
public class AssetImporter {
    
    public static final HashMap<String, Object> DEFAULTS = new HashMap<String, Object>();
    static {
        DEFAULTS.put("out-path", "/Models");
        DEFAULTS.put("out-texture-path", "/Textures");
        DEFAULTS.put("copy-textures", "true");
    }
    static int exportedObjects, totalObjects, blocks, blocksTotal;
    static StringBuilder output = new StringBuilder();
    public static void main(String[] args) {
        if(args == null || args.length == 0) {
            System.err.println("Usage: <filepath> [importblocks, ...]");
            System.exit(0);
        }
        HashSet<String> set = null;
        if(args.length > 1) {
            set = new HashSet<String>();
            for(int i = 1; i < args.length; i++) set.add(args[i].trim());
        }
        
        HashMap<String, Map<String, Object>> map = parse(Paths.get(args[0]));
        importAssets(map, set);
        
        System.err.println("-------Finished-------");
        System.err.println(output.toString());
       
        System.err.println("Exported " + blocks + "/" + blocksTotal + " blocks, " + 
                exportedObjects + "/" + totalObjects + " objects.");
    }
    
    public static final PropertyParser parser = new PropertyParser() {
        @Override
        public Map<String, Object> createMap(String blockname) {
            return new HashMap<String, Object>();
        }

        @Override
        public Object parse(String parent, String name, Object value) {
            if(name.equals("blendpath") && value instanceof String) {
                return new Object[] {value};
            }
            
            return value;
        }
    };
    
    public static HashMap<String, Map<String, Object>> parse(String text) {
        return PropertyReader.parse(text, parser);
    }
    public static HashMap<String, Map<String, Object>> parse(Path path) {
        try {
            return PropertyReader.parse(path, parser);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }
    
    public static void importAssets(HashMap<String, Map<String, Object>> map, HashSet<String> importBlocks) {
        Map<String, Object> def = map.get("global");
        if(def == null) def = DEFAULTS;
        
        HashMap<String, Object> tmp = new HashMap<String, Object>();
        
        for(Map.Entry<String, Map<String, Object>> item : map.entrySet()) {
            if(item.getKey().equals("global")) continue;
            blocksTotal++;
            if(importBlocks != null && !importBlocks.contains(item.getKey())) continue;
            blocks++;
            Map<String, Object> m = item.getValue();
            
            tmp.clear();
            tmp.putAll(def);
            tmp.putAll(m);
            
            ImportAsset(tmp);
        }
    }
    public static void ImportAsset(Map<String, Object> map) {
        try {
            Object[] inpath = (Object[])map.get("blendpath");
            String assetpath = (String)map.get("assetpath");
            String outpath = (String)map.get("out-path");
            String outtexpath = (String)map.get("out-texture-path");
            String filter = (String)map.get("filter");
            boolean copytextures = "true".equals(map.get("copy-texture"));
            boolean clean = "true".equals(map.get("clean"));

            String inblend = (String)map.get("in-blend");
            Object _outj3o = map.get("out-j3o");
           
            if(inblend != null && _outj3o != null && _outj3o.getClass().isArray()) {
                Object[] outj3o = (Object[]) _outj3o;       
                totalObjects += outj3o.length;
                
                File file = locateFile(inpath, inblend);
                if(file == null) {
                    output.append("Could not locate ").append(inblend).append('\n');
                    return;
                }
                
                
                Spatial sp = loadBlendFile(filter, copytextures, assetpath, outtexpath, file, getObjectNames(outj3o));
                System.err.println("Loading " + sp);
                if(sp != null) {
                    if(sp instanceof Node) {
                        Node n = (Node)sp;
                        for(int i = 0; i < outj3o.length; i++) {
                            Object[] item = (Object[])outj3o[i];
                            Spatial found = n.getChild(item[0].toString());
                            if(found != null) {
                                exportJ3o(found, new File(assetpath+outpath, item[1].toString()));
                            }
                        }
                    }
                    else {
                        for(int i = 0; i < outj3o.length; i++) {
                            Object[] item = (Object[])outj3o[i];
                            if(sp.getName().equals(item[0])) {
                                exportJ3o(sp, new File(assetpath+outpath, item[1].toString()));
                            }
                        }
                    }
                }
                else System.err.println("Blend file not loaded: " + file.getName());
            }
            else {
                System.out.println("Missing parameters");
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    static String[] getObjectNames(Object[] arr) {
        String[] names = new String[arr.length];
        for(int i = 0; i < names.length; i++) {
            names[i] = (String)((Object[])arr[i])[0];
        }
        return names;
    }
    static File locateFile(Object[] dirs, String filename) {
        for(int i = 0; i < dirs.length; i++) {
            File f = new File(dirs[i].toString(), filename);
            if(f.exists()) return f;
        }
        return null;
    }
    public static Spatial loadBlendFile(String filter, boolean copyTex, String assetpath, String texPath, File file, String... objects) throws Exception {
        BlendKeyPart key = new BlendKeyPart(file.getAbsolutePath(), objects);
        key.assetPath = assetpath;
        key.texturePath = texPath;
        key.copyTextures = copyTex;
        if(filter != null) {
            try {
                Class cls = Class.forName(filter);
                key.filter = (ImportFilter) cls.newInstance();
            }
            catch(Exception e) {
                System.err.println("Error: Could not initialize " + filter + ", using a default filter.");
                e.printStackTrace();
            }
        }
        key.setLoadUnlinkedAssets(false);
        
        Logger test = Logger.getLogger(TextureHelper.class.getName());
        test.setLevel(Level.FINE);
        test.getParent().getHandlers()[0].setLevel(Level.FINE);
        
        AssetManager am = JmeSystem.newAssetManager(JmeSystem.getPlatformAssetConfigURL());
        am.registerLoader(BlendImporter.class, "blend");

        Spatial sp = (Node)am.loadAssetFromStream(key,
                new FileInputStream(file));
        return sp;
    }
    public static void exportJ3o(Spatial s, File outFile) {
        s.setLocalTranslation(0, 0, 0);
        exportedObjects++;
        output.append("Export to: ").append(outFile.getAbsolutePath()).append('\n');
        try {
            BinaryExporter exporter = BinaryExporter.getInstance();
            exporter.save(s, outFile);
            System.err.println(outFile.getName() + " created.");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
