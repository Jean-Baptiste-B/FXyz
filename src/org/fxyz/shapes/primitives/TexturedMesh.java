package org.fxyz.shapes.primitives;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.TriangleMesh;
import org.fxyz.geometry.Point3D;
import org.fxyz.utils.DensityFunction;
import org.fxyz.utils.TriangleMeshHelper;
import static org.fxyz.utils.TriangleMeshHelper.DEFAULT_COLORS;
import static org.fxyz.utils.TriangleMeshHelper.DEFAULT_DENSITY_FUNCTION;
import static org.fxyz.utils.TriangleMeshHelper.DEFAULT_PATTERN_SCALE;
import org.fxyz.utils.TriangleMeshHelper.TextureType;

/**
 * TexturedMesh is a base class that provides support for different mesh implementations
 * taking into account four different kind of textures
 * - None
 * - Image
 * - Colored vertices
 * - Colored faces
 * 
 * For the last two ones, number of colors and density map have to be provided
 * 
 * Any subclass must use mesh, listVertices and listFaces
 * 
 * @author jpereda
 */
public abstract class TexturedMesh extends MeshView {
    
    private TriangleMeshHelper helper = new TriangleMeshHelper();
    protected TriangleMesh mesh;
    
    protected final List<Point3D> listVertices = new ArrayList<>();
    protected final List<Point3D> listTextures = new ArrayList<>();
    protected final List<Point3D> listFaces = new ArrayList<>();
    protected float[] textureCoords;
    
    protected final Rectangle rectMesh=new Rectangle(0,0);
    protected final Rectangle areaMesh=new Rectangle(0,0);
    
    protected TexturedMesh(){
        textureType.set(TextureType.NONE);
    }
    
    private final ObjectProperty<TextureType> textureType = new SimpleObjectProperty<TextureType>(){

        @Override
        protected void invalidated() {
            if(mesh!=null){
                updateTexture();
                updateTextureOnFaces();
            }
        }
        
    };

    public void setTextureModeNone() {
        helper.setTextureType(TextureType.NONE);
        setTextureType(TextureType.NONE);
    }
    
    public void setTextureModeNone(Color color) {
        if(color!=null){
            helper.setTextureType(TextureType.NONE);
            setMaterial(helper.getMaterialWithColor(color));
        }
        setTextureType(helper.getTextureType());
    }
    
    public void setTextureModeImage(String image) {
        if(image!=null && !image.isEmpty()){
            helper.setTextureType(TextureType.IMAGE);
            setMaterial(helper.getMaterialWithImage(image));
            setTextureType(helper.getTextureType());
        }
    }
    
    public void setTextureModePattern(double scale) {
        helper.setTextureType(TextureType.PATTERN);
        patternScale.set(scale);
        setMaterial(helper.getMaterialWithPattern());
        setTextureType(helper.getTextureType());
    }
    
    public void setTextureModeVertices(int colors, DensityFunction dens) {
        helper.setTextureType(TextureType.COLORED_VERTICES);
        setColors(colors);
        setDensity(dens);
        setTextureType(helper.getTextureType());
    }
    
    public void setTextureModeFaces(int colors) {
        helper.setTextureType(TextureType.COLORED_FACES);
        setColors(colors);
        setTextureType(helper.getTextureType());
    }
    
    public TextureType getTextureType() {
        return textureType.get();
    }

    public void setTextureType(TextureType value) {
        textureType.set(value);
    }

    public ObjectProperty textureTypeProperty() {
        return textureType;
    }
    
    private final DoubleProperty patternScale = new SimpleDoubleProperty(DEFAULT_PATTERN_SCALE){

        @Override
        protected void invalidated() {
            updateTexture();
        }
        
    };
    
    public final double getPatternScale(){
        return patternScale.get();
    }
    
    public final void setPatternScale(double scale){
        patternScale.set(scale);
    }
    
    public DoubleProperty patternScaleProperty(){
        return patternScale;
    }
    
    private final IntegerProperty colors = new SimpleIntegerProperty(DEFAULT_COLORS){

        @Override protected void invalidated() {
            createPalette(getColors());
            updateTexture();
            updateTextureOnFaces();
        }
    };

    public final int getColors() {
        return colors.get();
    }

    public final void setColors(int value) {
        colors.set(value);
    }

    public IntegerProperty colorsProperty() {
        return colors;
    }
    
    private final ObjectProperty<DensityFunction> density = new SimpleObjectProperty<DensityFunction>(DEFAULT_DENSITY_FUNCTION){
        
        @Override protected void invalidated() {
            helper.setDensity(density.get());
            updateTextureOnFaces();
        }
    };
    
    public final DensityFunction getDensity(){
        return density.get();
    }
    
    public final void setDensity(DensityFunction value){
        this.density.set(value);
    }
    
    public final ObjectProperty<DensityFunction> densityProperty() {
        return density;
    }

    private void createPalette(int colors) {
        helper.createPalette(colors,false);        
        setMaterial(helper.getMaterialWithPalette());
    }
    
    public void updateVertices(float factor){
        if(mesh!=null){
            mesh.getPoints().setAll(helper.updateVertices(listVertices, factor));
        }
    }
    private void updateTexture(){
        if(mesh!=null){
            switch(textureType.get()){
                case NONE: 
                    mesh.getTexCoords().setAll(0f,0f);
                    break;
                case IMAGE: 
                    mesh.getTexCoords().setAll(textureCoords);
                    break;
                case PATTERN: 
                    if(areaMesh.getHeight()>0 && areaMesh.getWidth()>0){
                        mesh.getTexCoords().setAll(
                            helper.updateTexCoordsWithPattern((int)rectMesh.getWidth(),
                                    (int)rectMesh.getHeight(),patternScale.get(),
                                    areaMesh.getHeight()/areaMesh.getWidth()));
                    } else {
                        mesh.getTexCoords().setAll(
                            helper.updateTexCoordsWithPattern((int)rectMesh.getWidth(),
                                    (int)rectMesh.getHeight(),patternScale.get()));
                    }
                    break;
                case COLORED_VERTICES:
                    mesh.getTexCoords().setAll(helper.getTexturePaletteArray());
                    break;
                case COLORED_FACES:
                    mesh.getTexCoords().setAll(helper.getTexturePaletteArray());
                    break;
            }
        }
    }
    
    private void updateTextureOnFaces(){
        // textures for level
        if(mesh!=null){
            switch(textureType.get()){
                case NONE: 
                    mesh.getFaces().setAll(helper.updateFacesWithoutTexture(listFaces));
                    break;
                case IMAGE: 
                    if(listTextures.size()>0){
                        mesh.getFaces().setAll(helper.updateFacesWithTextures(listFaces,listTextures));
                    } else { 
                        mesh.getFaces().setAll(helper.updateFacesWithVertices(listFaces));
                    }
                    break;
                case PATTERN: 
                    mesh.getFaces().setAll(helper.updateFacesWithTextures(listFaces,listTextures));
                    break;
                case COLORED_VERTICES:
                    mesh.getFaces().setAll(helper.updateFacesWithDensityMap(listVertices, listFaces));
                    break;
                case COLORED_FACES:
                    mesh.getFaces().setAll(helper.updateFacesWithFaces(listFaces));
                    break;
            }
        }
    }
    
    protected abstract void updateMesh();
    
    protected void createTexCoords(int width, int height){
        rectMesh.setWidth(width);
        rectMesh.setHeight(height);
        textureCoords=helper.createTexCoords(width, height);
    }
    
    protected TriangleMesh createMesh(){
        TriangleMesh triangleMesh = new TriangleMesh();
        triangleMesh.getPoints().setAll(helper.updateVertices(listVertices));
        triangleMesh.getTexCoords().setAll(0f,0f);
        triangleMesh.getFaces().setAll(helper.updateFacesWithoutTexture(listFaces));
        int[] faceSmoothingGroups = new int[listFaces.size()];
        Arrays.fill(faceSmoothingGroups, 1);
 
        triangleMesh.getFaceSmoothingGroups().addAll(faceSmoothingGroups);
        
        System.out.println("nodes: "+listVertices.size()+", faces: "+listFaces.size());
//        System.out.println("area: "+helper.getMeshArea(listVertices, listFaces));
        return triangleMesh;
    }
}
