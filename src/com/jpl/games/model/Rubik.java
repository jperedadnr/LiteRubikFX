package com.jpl.games.model;

import com.jpl.games.math.Rotations;
import com.jpl.games.model3d.Model3D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.SubScene;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

/**
 *
 * @author jpereda, April 2014 - @JPeredaDnr
 */
public class Rubik {
    
    private final Group cube=new Group();    
    private Map<String,MeshView> mapMeshes=new HashMap<>();
    private final double dimCube;
    
    private final ContentModel content; 
    
    private final Rotations rot;
    
    private List<Integer> order;
    private List<Integer> reorder, layer;
    private final DoubleProperty rotation=new SimpleDoubleProperty(0d);
    private final BooleanProperty onRotation=new SimpleBooleanProperty(false);
    private Point3D axis=new Point3D(0,0,0);
    private final ChangeListener<Number> rotMap;
    

    public Rubik(){
        /*
        Import Rubik's Cube model and arrows
        */
        Model3D model=new Model3D();
        model.importObj();
        mapMeshes=model.getMapMeshes();
        cube.getChildren().setAll(mapMeshes.values());
        dimCube=cube.getBoundsInParent().getWidth();
        
        /*
        Create content subscene, add cube, set camera and lights
        */
        content = new ContentModel(800,600,dimCube); 
        content.setContent(cube);

        /*
        Initialize 3D array of indexes and a copy of original/solved position
        */
        rot=new Rotations();
        order=rot.getCube();
        
        /*
        Listener to perform an animated face rotation.
        
        Note: by prepending the rotations it is not possible to create the animation with a timeline
        like this:
        Rotate r=new Rotate(0,axis);
        v.getTransforms().add(r);
        Timeline timeline=new Timeline();
        timeline.getKeyFrames().add(new KeyFrame(Duration.Seconds(2),new KeyValue(rotation.angle,90)));
        that takes care of the values: 0<=angle<=90º and transforms the cubies smoothly.
        
        So we create the timeline, and listen to how internally it interpolate rotate.angle and perform
        small rotations between the increments of the angle that the timeline generates:
        */
        rotMap=(ov,angOld,angNew)->{ 
            mapMeshes.forEach((k,v)->{
                layer.stream().filter(l->k.contains(l.toString()))
                    .findFirst().ifPresent(l->{
                        Affine a=new Affine(v.getTransforms().get(0));
                        a.prepend(new Rotate(angNew.doubleValue()-angOld.doubleValue(),axis));
                        v.getTransforms().setAll(a);
                        if(k.equals("Block46")){
                            System.out.println("ang: "+(angNew.doubleValue()-angOld.doubleValue()));
                        }
                    });
            });
        };
    }
    
    // called on toolbars buttons click
    public void rotateFace(final String btRot){
        if(onRotation.get()){
            return;
        }
        onRotation.set(true);
        
        boolean bFace= !(btRot.startsWith("X")||btRot.startsWith("Y")||btRot.startsWith("Z"));

        // rotate cube indexes
        rot.turn(btRot);
        // get new indexes in terms of blocks numbers from original order
        reorder=rot.getCube();

        // select cubies to rotate: those in reorder different from order.
        if(!bFace){
            layer=reorder.stream().collect(Collectors.toList());
        }else {
            AtomicInteger index = new AtomicInteger();
            layer=order.stream()
                        .filter(o->!Objects.equals(o, reorder.get(index.getAndIncrement())))
                        .collect(Collectors.toList());
            // add central cubie
            layer.add(0,reorder.get(Utils.getCenter(btRot)));
        }
        // set rotation axis            
        axis=Utils.getAxis(btRot); 
        // define rotation
        double angEnd=90d*(btRot.endsWith("i")?1d:-1d);
        
        rotation.set(0d);
        rotation.addListener(rotMap);

        // create animation
        Timeline timeline=new Timeline();
        timeline.getKeyFrames().add(
            new KeyFrame(Duration.millis(600), e->{
                    rotation.removeListener(rotMap);
                    onRotation.set(false); 
                },  new KeyValue(rotation,angEnd)));
        timeline.playFromStart();

        // update order with last list, to start all over again in the next rotation
        order=reorder.stream().collect(Collectors.toList());
    }

    public SubScene getSubScene(){ return content.getSubScene(); }
    public BooleanProperty isOnRotation() { return onRotation; }
    
}
