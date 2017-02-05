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
package jme.blend.data.io.blend;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.AssetLocator;
import com.jme3.asset.AssetManager;
import com.jme3.asset.BlenderKey;
import com.jme3.asset.ModelKey;
import com.jme3.asset.StreamAssetInfo;
import com.jme3.asset.TextureKey;
import com.jme3.light.Light;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.renderer.Camera;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.LightNode;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.animations.AnimationHelper;
import com.jme3.scene.plugins.blender.cameras.CameraHelper;
import com.jme3.scene.plugins.blender.constraints.ConstraintHelper;
import com.jme3.scene.plugins.blender.curves.CurvesHelper;
import com.jme3.scene.plugins.blender.file.BlenderFileException;
import com.jme3.scene.plugins.blender.file.BlenderInputStream;
import com.jme3.scene.plugins.blender.file.FileBlockHeader;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.landscape.LandscapeHelper;
import com.jme3.scene.plugins.blender.lights.LightHelper;
import com.jme3.scene.plugins.blender.materials.MaterialHelper;
import com.jme3.scene.plugins.blender.meshes.MeshHelper;
import com.jme3.scene.plugins.blender.modifiers.ModifierHelper;
import com.jme3.scene.plugins.blender.objects.ObjectHelper;
import com.jme3.scene.plugins.blender.particles.ParticlesHelper;
import com.jme3.scene.plugins.blender.textures.TextureHelper;
import com.jme3.shader.VarType;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Juraj Papp
 */
public class BlendImporter implements AssetLoader {
    private static final Logger LOGGER = Logger.getLogger(BlendImporter.class.getName());
   
    @Override
    public Spatial load(AssetInfo assetInfo) throws IOException {
        try {
            AssetKey key = assetInfo.getKey();
            if(!(key instanceof BlendKeyPart)) return null;
            final BlendKeyPart keypart = (BlendKeyPart)key;
                        
            BlenderContext blenderContext = this.setup(assetInfo);

            AnimationHelper animationHelper = blenderContext.getHelper(AnimationHelper.class);
            animationHelper.loadAnimations();
            BlenderKey blenderKey = blenderContext.getBlenderKey();
            LoadedFeatures loadedFeatures = new LoadedFeatures();
            for (FileBlockHeader block : blenderContext.getBlocks()) {
                switch (block.getCode()) {
                    case BLOCK_OB00:
                        ObjectHelper objectHelper = blenderContext.getHelper(ObjectHelper.class);
                        Structure structure = block.getStructure(blenderContext);
                        
                        String name = structure.getName();
                        
                        if(!keypart.loadObjects.contains(name)) continue;
                        LOGGER.log(Level.INFO, "Loading {0}", name);
                        
                        Node object = (Node) objectHelper.toObject(structure, blenderContext);
                        if (LOGGER.isLoggable(Level.FINE)) {
                            LOGGER.log(Level.FINE, "{0}: {1}--> {2}", new Object[] { object.getName(), object.getLocalTranslation().toString(), object.getParent() == null ? "null" : object.getParent().getName() });
                        }
//                        if (object.getParent() == null) {
                            loadedFeatures.objects.add(object);
//                        }
                        if (object instanceof LightNode && ((LightNode) object).getLight() != null) {
                            loadedFeatures.lights.add(((LightNode) object).getLight());
                        } else if (object instanceof CameraNode && ((CameraNode) object).getCamera() != null) {
                            loadedFeatures.cameras.add(((CameraNode) object).getCamera());
                        }
                        break;

                    default:
                        LOGGER.log(Level.FINEST, "Ommiting the block: {0}.", block.getCode());
                }
            }

            ConstraintHelper constraintHelper = blenderContext.getHelper(ConstraintHelper.class);
            constraintHelper.bakeConstraints(blenderContext);

            Node modelRoot = new Node(blenderKey.getName());

            for (Node object : loadedFeatures.objects) {
                if(keypart.copyTextures) {
                    object.depthFirstTraversal(new SceneGraphVisitor() {
                        @Override
                        public void visit(Spatial object) {
                            if(object instanceof Geometry) {
                                Geometry g = (Geometry)object;
                                Material mat = g.getMaterial();
                                for(Map.Entry<String, MatParam> e : mat.getParamsMap().entrySet()) {

                                    MatParam param = e.getValue();
                                    if(param.getVarType() == VarType.Texture2D) {
                                        importTexture(keypart.filter, keypart.assetPath, keypart.texturePath, param);
                                    }
                                }
                            }
                        }
                    });
                }
                Spatial sp = keypart.filter.importSpatial(object);
                if(sp != null) modelRoot.attachChild(sp);
            }
            return modelRoot;
        } catch (BlenderFileException e) {
            throw new IOException(e.getLocalizedMessage(), e);
        } catch (Exception e) {
            throw new IOException("Unexpected importer exception occured: " + e.getLocalizedMessage(), e);
        } finally {
            this.clear(assetInfo);
        }
    }
    protected void clear(AssetInfo assetInfo) {
        assetInfo.getManager().unregisterLocator(assetInfo.getKey().getName(), LinkedContentLocator.class);
    }
    void importTexture(ImportFilter filter, String assetPath, String texPath, MatParam param) {
        if(param.getVarType() == VarType.Texture2D) {
            Texture2D tex = (Texture2D)param.getValue();
            if(tex != null) {
                LOGGER.log(Level.INFO, "Loading Texture {0}", tex.getName());

                Image img = tex.getImage();
                if(img != null) {
                    
                    String path = "/"+tex.getKey().getName();
                    File texFile = new File(path);
                    if(!texFile.exists()) return;
                    String filename = texFile.getName();
                    
                    File texAssetPath = new File(assetPath+texPath, filename);
                   
                    if(!texAssetPath.exists() || Math.abs(texFile.lastModified()-texAssetPath.lastModified()) > 1000 ) {
                        LOGGER.log(Level.INFO, "Copying texture {0}", tex.getName());
                        filename = filter.importTexture(tex, texFile, assetPath+texPath);
                    }
                    if(filename == null) {
                        LOGGER.log(Level.INFO, "Warning: texture import returned null.");
                    }
                    TextureKey texKey = (TextureKey)tex.getKey();
                    set(texKey, "name", new File(texPath, filename).getAbsolutePath());
                }
            }
            
        }
    }
    
    private static void set(Object o, String name, String value) {
        try {
            Field f = AssetKey.class.getDeclaredField(name);
            f.setAccessible(true);
            f.set(o, value);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    protected BlenderContext setup(AssetInfo assetInfo) throws BlenderFileException {
        ModelKey modelKey = (ModelKey) assetInfo.getKey();
        BlenderKey blenderKey;
        if (modelKey instanceof BlenderKey) {
            blenderKey = (BlenderKey) modelKey;
        } else {
            blenderKey = new BlenderKey(modelKey.getName());
        }
        
        BlenderInputStream inputStream = new BlenderInputStream(assetInfo.openStream());

        List<FileBlockHeader> blocks = new ArrayList<FileBlockHeader>();
        FileBlockHeader fileBlock;
        BlenderContext blenderContext = new BlenderContext();
        blenderContext.setBlenderVersion(inputStream.getVersionNumber());
        blenderContext.setAssetManager(assetInfo.getManager());
        blenderContext.setInputStream(inputStream);
        blenderContext.setBlenderKey(blenderKey);

        blenderContext.putHelper(AnimationHelper.class, new AnimationHelper(inputStream.getVersionNumber(), blenderContext));
        blenderContext.putHelper(TextureHelper.class, new TextureHelper(inputStream.getVersionNumber(), blenderContext));
        blenderContext.putHelper(MeshHelper.class, new MeshHelper(inputStream.getVersionNumber(), blenderContext));
        blenderContext.putHelper(ObjectHelper.class, new ObjectHelper(inputStream.getVersionNumber(), blenderContext));
        blenderContext.putHelper(CurvesHelper.class, new CurvesHelper(inputStream.getVersionNumber(), blenderContext));
        blenderContext.putHelper(LightHelper.class, new LightHelper(inputStream.getVersionNumber(), blenderContext));
        blenderContext.putHelper(CameraHelper.class, new CameraHelper(inputStream.getVersionNumber(), blenderContext));
        blenderContext.putHelper(ModifierHelper.class, new ModifierHelper(inputStream.getVersionNumber(), blenderContext));
        blenderContext.putHelper(MaterialHelper.class, new MaterialHelper(inputStream.getVersionNumber(), blenderContext));
        blenderContext.putHelper(ConstraintHelper.class, new ConstraintHelper(inputStream.getVersionNumber(), blenderContext));
        blenderContext.putHelper(ParticlesHelper.class, new ParticlesHelper(inputStream.getVersionNumber(), blenderContext));
        blenderContext.putHelper(LandscapeHelper.class, new LandscapeHelper(inputStream.getVersionNumber(), blenderContext));

        FileBlockHeader sceneFileBlock = null;
        do {
            fileBlock = new FileBlockHeader(inputStream, blenderContext);
            if (!fileBlock.isDnaBlock()) {
                blocks.add(fileBlock);
                if (fileBlock.getCode() == FileBlockHeader.BlockCode.BLOCK_SC00) {
                    sceneFileBlock = fileBlock;
                }
            }
        } while (!fileBlock.isLastBlock());
        if (sceneFileBlock != null) {
            blenderContext.setSceneStructure(sceneFileBlock.getStructure(blenderContext));
        }
        
        assetInfo.getManager().registerLocator(assetInfo.getKey().getName(), LinkedContentLocator.class);
        
        return blenderContext;
    }
    
    
    
    private static class LoadedFeatures {
        private List<Node>            objects         = new ArrayList<Node>();
        private List<Camera>          cameras         = new ArrayList<Camera>();
        private List<Light>           lights          = new ArrayList<Light>();
    }
    public static class LinkedContentLocator implements AssetLocator {
        private File rootFolder;
        
        @Override
        public void setRootPath(String rootPath) {
            rootFolder = new File(rootPath);
            if(rootFolder.isFile()) {
                rootFolder = rootFolder.getParentFile();
            }
        }

        @SuppressWarnings("rawtypes")
        @Override
        public AssetInfo locate(AssetManager manager, AssetKey key) {
            if(key instanceof BlenderKey || key instanceof TextureKey) {
                File linkedAbsoluteFile = new File("/"+key.getName());
                if(linkedAbsoluteFile.exists() && linkedAbsoluteFile.isFile()) {
                    try {
                        return new StreamAssetInfo(manager, key, new FileInputStream(linkedAbsoluteFile));
                    } catch (FileNotFoundException e) {
                        return null;
                    }
                }
                
                File linkedFileInCurrentAssetFolder = new File(rootFolder, linkedAbsoluteFile.getName());
                if(linkedFileInCurrentAssetFolder.exists() && linkedFileInCurrentAssetFolder.isFile()) {
                    try {
                        return new StreamAssetInfo(manager, key, new FileInputStream(linkedFileInCurrentAssetFolder));
                    } catch (FileNotFoundException e) {
                        return null;
                    }
                }
                
                File linkedFileInCurrentFolder = new File(".", linkedAbsoluteFile.getName());
                if(linkedFileInCurrentFolder.exists() && linkedFileInCurrentFolder.isFile()) {
                    try {
                        return new StreamAssetInfo(manager, key, new FileInputStream(linkedFileInCurrentFolder));
                    } catch (FileNotFoundException e) {
                        return null;
                    }
                }
            }
            return null;
        }
    }
}
