package org.fxyz.utils;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import org.fxyz.geometry.Point3D;

/**
 *
 * @author jpereda
 */
public class TriangleMeshHelper {
    
    public static enum TextureType {
        NONE, // without texture, simple colored
        IMAGE, // an image is loaded 
        PATTERN, // an image from a pattern
        COLORED_FACES, // a palette is used to color faces
        COLORED_VERTICES // a palette is used to color vertices with a density map
    }
    public static final TextureType DEFAULT_TEXTURE_TYPE= TextureType.NONE;
    private TextureType textureType=DEFAULT_TEXTURE_TYPE;
    
    public TriangleMeshHelper(){
    }
    
    public void setTextureType(TextureType textureType){
        this.textureType = textureType;
        
        switch(textureType){
            case COLORED_FACES:
            case COLORED_VERTICES:
                createPalette();
                density=DEFAULT_DENSITY_FUNCTION;        
                break;
            case PATTERN: 
                createPattern();
                break;
        }
    }
    
    public TextureType getTextureType() { return textureType; }
    
    /*
    Patterns
    */
    public static final double DEFAULT_PATTERN_SCALE = 0d;
    public static final int DEFAULT_WIDTH =  12;
    public static final int DEFAULT_HEIGHT = 12;
    public final static boolean DEFAULT_SAVE_PATTERN = false;
    private Patterns patterns;
    private int patternWidth;
    private int patternHeight;
    
    public final void createPattern(){
        createPattern(DEFAULT_WIDTH,DEFAULT_HEIGHT,DEFAULT_SAVE_PATTERN);
    }
    public void createPattern(boolean save){
        createPattern(DEFAULT_WIDTH,DEFAULT_HEIGHT,save);
    }
    public void createPattern(int width, int height, boolean save){
        this.patternWidth=width;
        this.patternHeight=height;
        patterns=new Patterns(width,height);
        patterns.createPattern(save);
    }
    public Image getPatternImage() {
        if(patterns==null){
            createPattern();
        }
        return patterns.getPatternImage();
    }
    
    public Material getMaterialWithPattern(){
        PhongMaterial mat = new PhongMaterial();
        mat.setDiffuseMap(getPatternImage());
        return mat;
    }
    
    /*
    Colors, palette
    */
    public final static int DEFAULT_COLORS = 16;
    public final static boolean DEFAULT_SAVE_PALETTE = false;
    private Palette palette;
    private int colors;
    
    public final void createPalette(){
        createPalette(DEFAULT_COLORS,DEFAULT_SAVE_PALETTE);
    }
    public void createPalette(int colors){
        createPalette(colors,DEFAULT_SAVE_PALETTE);
    }
    public void createPalette(boolean save){
        createPalette(DEFAULT_COLORS,save);
    }
    public void createPalette(int colors, boolean save){
        this.colors=colors;
        palette=new Palette(colors);
        palette.createPalette(save);
    }

    public Image getPaletteImage() {
        if(palette==null){
            createPalette();
        }
        return palette.getPaletteImage();
    }
    
    public Material getMaterialWithPalette(){
        PhongMaterial mat = new PhongMaterial();
        mat.setDiffuseMap(getPaletteImage());
        return mat;
    }
    
    public Material getMaterialWithColor(Color color){
        PhongMaterial mat = new PhongMaterial(color);
        return mat;
    }
    
    public float[] getTexturePaletteArray(){
        if(palette==null){
            createPalette();
        }
        return IntStream.range(0,colors).boxed()
            .flatMapToDouble(palette::getTextureLocation)
            .collect(()->new FloatCollector(2*colors), FloatCollector::add, FloatCollector::join)
            .toArray();
    }
    
    /*
    density function
    */
    public final static DensityFunction DEFAULT_DENSITY_FUNCTION= p->0;
    private DensityFunction density;
    private double min = 0d;
    private double max = 1d;
    
    public void setDensity(DensityFunction density){
        this.density=density;
    }
    
    public int mapDensity(Point3D p){
        int f=(int)((density.eval(p)-min)/(max-min)*colors);
        if(f<0){
            f=0;
        }
        if(f>=colors){
            f=colors-1;
        }
        return f;
    }

    public int mapFaces(int face, int numFaces){
        int f=(int)((((double)face)/((double)numFaces)) *colors);
        if(f<0){
            f=0;
        }
        if(f>=colors){
            f=colors-1;
        }
        return f;
    }

    public void updateExtremes(List<Point3D> points){
        max=points.parallelStream().mapToDouble(p->density.eval(p)).max().orElse(1.0);
        min=points.parallelStream().mapToDouble(p->density.eval(p)).min().orElse(0.0);
        if(max==min){
            max=1.0+min;
        }
//        System.out.println("Min: "+min+", max: "+max);  
    }
    
    /*
    image
    */
    public Material getMaterialWithImage(String image){
        PhongMaterial mat = new PhongMaterial();
        mat.setDiffuseMap(new Image(image));
        return mat;
    }
    
    /*
    Mesh updating
    */
    public float[] updateVertices(List<Point3D> points){
        return points.stream()
            .flatMapToDouble(Point3D::getCoordinates)
            .collect(()->new FloatCollector(points.size()*3), FloatCollector::add, FloatCollector::join)
            .toArray();       
    }
    
    public float[] updateVertices(List<Point3D> points, float factor){
        return points.stream()
            .flatMapToDouble(p->p.getCoordinates(factor))
            .collect(()->new FloatCollector(points.size()*3), FloatCollector::add, FloatCollector::join)
            .toArray();       
    }
    
    public float[] createTexCoords(int width, int height){
        int index=0;
        float[] textureCoords = new float[(width+1)*(height+1)*2];
        for (int y = 0; y <= height; y++) {
            float dy = (float) y / ((float)(height));
            for (int x = 0; x <= width; x++) {
                textureCoords[index] = (float) x /((float)(width));
                textureCoords[index + 1] = dy;
                index+=2;
            }
        }
        return textureCoords;
    }
    
    public float[] updateTexCoordsWithPattern(int rectWidth, int rectHeight){
        return updateTexCoordsWithPattern(rectWidth, rectHeight, 1d, 1d);
    }
    
    public float[] updateTexCoordsWithPattern(int rectWidth, int rectHeight, double scale){
        return updateTexCoordsWithPattern(rectWidth, rectHeight, scale, 1d);
    }
    
    public float[] updateTexCoordsWithPattern(int rectWidth, int rectHeight, double scale, double ratio){
        int index=0;
        float[] textureCoords = new float[(rectWidth+1)*(rectHeight+1)*2];
        float restHeight=patternHeight-((float)(1d/(patternHeight/scale)*ratio*rectWidth))%patternHeight;
        float factorHeight = (float)(1d+restHeight/(1d/(patternHeight/scale)*ratio*rectWidth));
        float restWidth=patternWidth-((float)(rectWidth/(patternWidth/scale)))%patternWidth;
        float factorWidth = (float)(1d+restWidth/(rectWidth/(patternWidth/scale)));
        
        for (int y = 0; y <= rectHeight; y++) {
            float dy = (float) ((y)/(patternHeight/scale)*ratio/rectHeight*rectWidth*factorHeight);
            for (int x = 0; x <= rectWidth; x++) {
                textureCoords[index] = (float) ((x)/(patternWidth/scale)*factorWidth);
                textureCoords[index + 1] = dy;
                index+=2;
            }
        }
        return textureCoords;
    }
    
    public int[] updateFacesWithoutTexture(List<Point3D> faces){
        return faces.parallelStream().map(f->{
                int p0=(int)f.x; int p1=(int)f.y; int p2=(int)f.z;
                return IntStream.of(p0, 0, p1, 0, p2, 0);
            }).flatMapToInt(i->i).toArray();
    }
    
    public int[] updateFacesWithVertices(List<Point3D> faces){
        return faces.parallelStream().map(f->{
                int p0=(int)f.x; int p1=(int)f.y; int p2=(int)f.z;
                return IntStream.of(p0, p0, p1, p1, p2, p2);
            }).flatMapToInt(i->i).toArray();
    }
    
    public int[] updateFacesWithTextures(List<Point3D> faces, List<Point3D> textures){
        if(faces.size()>textures.size()){
            return null;
        }
        AtomicInteger count=new AtomicInteger();
        return faces.stream().map(f->{
                Point3D t=textures.get(count.getAndIncrement());
                int p0=(int)f.x; int p1=(int)f.y; int p2=(int)f.z;
                int t0=(int)t.x; int t1=(int)t.y; int t2=(int)t.z;
                return IntStream.of(p0, t0, p1, t1, p2, t2);
            }).flatMapToInt(i->i).toArray();
    }
    
    public int[] updateFacesWithDensityMap(List<Point3D> points, List<Point3D> faces){
        updateExtremes(points);
        return faces.parallelStream().map(f->{
                int p0=(int)f.x; int p1=(int)f.y; int p2=(int)f.z;
                int t0=mapDensity(points.get(p0));
                int t1=mapDensity(points.get(p1));
                int t2=mapDensity(points.get(p2));
                return IntStream.of(p0, t0, p1, t1, p2, t2);
            }).flatMapToInt(i->i).toArray();
    }
       
    public int[] updateFacesWithFaces(List<Point3D> faces){
        AtomicInteger count=new AtomicInteger();
        return faces.stream().map(f->{
                int p0=(int)f.x; int p1=(int)f.y; int p2=(int)f.z;
                int t0=mapFaces(count.getAndIncrement(),faces.size());
                return IntStream.of(p0, t0, p1, t0, p2, t0);
            }).flatMapToInt(i->i).toArray();
    }
    /*
    utils
    */
    public double getMeshArea(List<Point3D> points, List<Point3D> faces){
        DoubleProperty area = new SimpleDoubleProperty();
        faces.forEach(f->{
                int p0=(int)f.x; int p1=(int)f.y; int p2=(int)f.z;
                Point3D a = points.get(p0);
                Point3D b = points.get(p1);
                Point3D c = points.get(p2);
                area.set(area.get()+b.substract(a).crossProduct((c.substract(a))).magnitude()/2.0);
            });
        return area.get();
    }
}
