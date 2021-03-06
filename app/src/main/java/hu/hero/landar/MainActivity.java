package hu.hero.landar;

import android.animation.ObjectAnimator;
import android.net.Uri;
import android.os.Bundle;
import android.util.ArraySet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import com.google.android.filament.gltfio.Animator;
import com.google.android.filament.gltfio.FilamentAsset;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.ar.core.Anchor;
import com.google.ar.core.Config;
import com.google.ar.core.Earth;
import com.google.ar.core.GeospatialPose;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.SceneView;
import com.google.ar.sceneform.Sceneform;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.QuaternionEvaluator;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.CameraStream;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.BaseArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentOnAttachListener;
import hu.hero.landar.Geo.Point3;
import hu.hero.landar.common.JumpingNode;
import hu.hero.landar.common.RotatingNode;
import hu.hero.landar.database.PICDATA;
import hu.hero.landar.database.PICDATA3D;
import hu.hero.landar.helpers.GeoPermissionsHelper;
import hu.hero.landar.helpers.MapTouchWrapper;
import hu.hero.landar.helpers.MapView;
import hu.hero.landar.helpers.RoundCornerLayout;
import hu.hero.landar.net.GetDataByDistance;

public class MainActivity extends AppCompatActivity implements
        FragmentOnAttachListener,
        BaseArFragment.OnTapArPlaneListener,
        BaseArFragment.OnSessionConfigurationListener,
        ArFragment.OnViewCreatedListener{
    private static final String TAG = MainActivity.class.getSimpleName();
    private MainActivity mActivity;
    private static final double MIN_OPENGL_VERSION = 3.0;
    private ArFragment arFragment;
    private Renderable mPicModel;
    private Renderable mBPointModel;
    private Renderable mYellowTubeModel;
    private Renderable mTubeModel;
    private Renderable mBlueTubeModel;
    private ViewRenderable mViewRenderable;

    public MapView mMapView = null;
    private AnchorNode mLastAnchor = null;
    private Earth mEarth = null;

    private double mBaseLat = 0;
    private double mBaseLon = 119.0;

    private class PicAnchar extends Anchor{
        public LatLng pos;
    }

    private Earth getEarth(){
        if( arFragment != null ) {
            ArSceneView sceneview = arFragment.getArSceneView();
            if (sceneview != null) {
                Session session = sceneview.getSession();
                if (session != null) {
                    return session.getEarth();
                }
            }
        }
        return null;
    }

    private boolean getEarthTrackingState(){
        mEarth = getEarth();
        if( mEarth == null )
            return false;
        return mEarth.getTrackingState() == TrackingState.TRACKING;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = this;

        setContentView(R.layout.activity_main);
        getSupportFragmentManager().addFragmentOnAttachListener(this);

        if (savedInstanceState == null) {
            if (Sceneform.isSupported(this)) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.arFragment, ArFragment.class, null)
                        .commit();
            }
        }

        loadModels();

        // ??????????????????????????????
//        MapTouchWrapper mapTouchWrapper = findViewById(R.id.map_wrapper);
        RoundCornerLayout mapTouchWrapper = findViewById(R.id.map_wrapper);
        mapTouchWrapper.setCornerEnabled(true,true,false,false);
/*        mapTouchWrapper.setup(screenLocation -> {
            LatLng latLng = mMapView.googleMap.getProjection().fromScreenLocation(screenLocation);
//            Log.d("?????????",latLng.toString());
            if ( getEarthTrackingState() ){
                double altitude = mEarth.getCameraGeospatialPose().getAltitude() ;
                // The rotation quaternion of the anchor in the East-Up-South (EUS) coordinate system.
                float qx = 0f;
                float qy = 0f;
                float qz = 0f;
                float qw = 1f;
                Anchor anchor = mEarth.createAnchor(latLng.latitude, latLng.longitude, altitude, qx, qy, qz, qw);
                addAnchor(anchor, latLng );
            }
            mActivity.mMapView.addPicMarker( latLng );
        });*/

/*
        SupportMapFragment mapFragment = (SupportMapFragment) mActivity.getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull GoogleMap googleMap) {
                mMapView = new MapView(mActivity, googleMap);
            }
        });
*/
    }

    @Override
    public void onAttachFragment(@NonNull FragmentManager fragmentManager, @NonNull Fragment fragment) {
        if (fragment.getId() == R.id.arFragment) {
            arFragment = (ArFragment) fragment;
            arFragment.setOnSessionConfigurationListener(this);
            arFragment.setOnViewCreatedListener(this);
            arFragment.setOnTapArPlaneListener(this);
        }
    }

    @Override
    public void onSessionConfiguration(Session session, Config config) {
        if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
            config.setDepthMode(Config.DepthMode.AUTOMATIC);
        }

        if (session.isGeospatialModeSupported( Config.GeospatialMode.ENABLED)) {
            config.setGeospatialMode(Config.GeospatialMode.ENABLED);
        }
    }

    @Override
    public void onViewCreated(ArSceneView arSceneView) {
        arFragment.setOnViewCreatedListener(null);
        // Fine adjust the maximum frame rate
        arSceneView.setFrameRateFactor(SceneView.FrameRate.FULL);

        // Available modes: DEPTH_OCCLUSION_DISABLED, DEPTH_OCCLUSION_ENABLED
        // ?????????????????????
//        arSceneView.getCameraStream().setDepthOcclusionMode(
//                       CameraStream.DepthOcclusionMode.DEPTH_OCCLUSION_ENABLED );
        arSceneView.getCameraStream().setDepthOcclusionMode(
                        CameraStream.DepthOcclusionMode.DEPTH_OCCLUSION_DISABLED );

        // ??????????????????????????????
        arSceneView.getPlaneRenderer().setVisible(false);

        // ?????? scene ???????????????????????????
        arSceneView.getScene().addOnUpdateListener(frameTime->{
            if ( getEarthTrackingState() ){
                // TODO: the Earth object may be used here.
                GeospatialPose cameraGeospatialPose = mEarth.getCameraGeospatialPose();
                double lat = cameraGeospatialPose.getLatitude();
                double lon = cameraGeospatialPose.getLongitude();
                double altitute = cameraGeospatialPose.getAltitude();
                double heading = cameraGeospatialPose.getHeading();

                if( mMapView != null )
                    mMapView.updateMapPosition( lat, lon, heading );

                // ??????????????????, ???????????????????????????
                if( distance( cameraGeospatialPose.getLatitude(), cameraGeospatialPose.getLongitude(),
                            mBaseLat , mBaseLon ) > 50 ){
                    System.out.println("??????????????????, ???????????????????????????" );
                    mBaseLat = cameraGeospatialPose.getLatitude();
                    mBaseLon = cameraGeospatialPose.getLongitude();
                    GetDataByDistance GDBD = new GetDataByDistance(mActivity);
                    GDBD.get( "VD01" , mBaseLat, mBaseLon, 100  );
                }


                // Draw the placed anchor, if it exists.
//            if( mEarthAnchor != null )
//                renderCompassAtAnchor( render , mEarthAnchor );
//                for (Anchor anchor:mAnchors) {
//                    renderCompassAtAnchor( render , anchor );
                }
        });
    }

    public void loadModels() {

        CompletableFuture<ModelRenderable> marker = ModelRenderable
                .builder()
                .setSource(this
                        , Uri.parse("models/scene.gltf"))
                .setIsFilamentGltf(true)
                .setAsyncLoadEnabled(true)
                .build();
        CompletableFuture.allOf(marker)
                .handle((ok, ex) -> {
                    try {
                        mPicModel = marker.get();
                    } catch (InterruptedException | ExecutionException ignore) {

                    }
                    return null;
                });



        WeakReference<MainActivity> weakActivity = new WeakReference<>(this);
        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.RED))
                .thenAccept(
                        material -> {
                            // ????????????
                            material.setFloat(MaterialFactory.MATERIAL_METALLIC, 1f);
                            // ??????
                            mBPointModel = ShapeFactory.makeCylinder(0.5f, 10f,  new Vector3(0.0f, 0.15f, 0.0f), material);
                        });
        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.YELLOW))
                .thenAccept(
                        material -> {
                            // ????????????
                            material.setFloat(MaterialFactory.MATERIAL_METALLIC, 1f);
                            // ??????
                            mYellowTubeModel = ShapeFactory.makeCylinder(0.5f, 23f,  new Vector3(0.0f, 0.15f, 0.0f), material);
                        });
        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.BLUE))
                .thenAccept(
                        material -> {
                            // ????????????
                            material.setFloat(MaterialFactory.MATERIAL_METALLIC, 1f);
                            // ??????
                            mBlueTubeModel = ShapeFactory.makeCylinder(0.5f, 3f,  new Vector3(0.0f, 0.15f, 0.0f), material);
                        });
        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.rgb(255,255,0)))
                .thenAccept(
                        material -> {
                            // ????????????
                            material.setFloat(MaterialFactory.MATERIAL_METALLIC, 1f);
                            // ??????
                            mTubeModel = ShapeFactory.makeCylinder(0.5f, 10f,  new Vector3(0.0f, 0.15f, 0.0f), material);
                        });
        ViewRenderable.builder()
                .setView(this, R.layout.view_model_title)
                .build()
                .thenAccept(viewRenderable -> {
                    MainActivity activity = weakActivity.get();
                    if (activity != null) {
                        activity.mViewRenderable = viewRenderable;
                    }
                })
                .exceptionally(throwable -> {
                    Toast.makeText(this, "Unable to load model", Toast.LENGTH_LONG).show();
                    return null;
                });
        loadBPointModel();
    }

    /*
        ??????????????? Model
    */
    private void loadBPointModel(){
        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.RED))
                .thenAccept(
                        material -> {
                            // ????????????
                            material.setFloat(MaterialFactory.MATERIAL_METALLIC, 1f);
                            // ??????
//                            mBPointModel = ShapeFactory.makeSphere(0.75f, new Vector3(0.0f, 0.15f, 0.0f), material);
                            // ??????
                            mBPointModel = ShapeFactory.makeCylinder(1.0f, 5f,  new Vector3(0.0f, 0.15f, 0.0f), material);
                        });
    }

    /*
        ?????????????????????
    */
    @Override
    public void onTapPlane(HitResult hitResult, Plane plane, MotionEvent motionEvent) {
        Anchor anchor = hitResult.createAnchor();
        AnchorNode baseNode = new AnchorNode(anchor);
        JumpingNode jNode = new JumpingNode();
        jNode.setParent( baseNode );
        RotatingNode picNode = new RotatingNode(false);
        picNode.setRenderable( mPicModel );
        picNode.setParent( jNode );
        picNode.setLocalScale(new Vector3(0.2f, 0.2f, 0.2f));
        arFragment.getArSceneView().getScene().addChild(baseNode);
    }

    /*
        ???????????????????????????
    */
    private Node createPicNode( Node baseNode ){
        JumpingNode jNode = new JumpingNode();
        jNode.setParent( baseNode );
        RotatingNode picNode = new RotatingNode(false);
        picNode.setRenderable( mPicModel );
        picNode.setLocalScale(new Vector3(1f, 1f, 1f));
        picNode.setParent( jNode );
        return picNode;
    }
    /*
       ???????????????????????????
    */
    public void onReadDataFinish( GetDataByDistance.SpatialIndexPackage pack ){
        if( pack == null )
        {
            Toast.makeText(this, "??????????????????????????????", Toast.LENGTH_LONG)
                    .show();
            return;
        }

        List <PICDATA3D> picList = pack.pics;
        List<List<Point3>>  ptLists = pack.ptlists;
        if( getEarthTrackingState() ){
            // The rotation quaternion of the anchor in the East-Up-South (EUS) coordinate system.
            float qx = 0f;
            float qy = 0f;
            float qz = 0f;
            float qw = 1f;
/*
            // ??????
            for( List<Point3> list :ptLists ) {
                AnchorNode lastNode=null;
                AnchorNode firstNode=null;
                for( int i=0 ; i<list.size() ; i++ ){
                    Point3 p = list.get(i);
                    Anchor anchor = mEarth.createAnchor( p.y, p.x, p.h+3, qx, qy, qz, qw);
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(arFragment.getArSceneView().getScene());
                    // Create the transformable model and add it to the anchor.
                    TransformableNode model = new TransformableNode(arFragment.getTransformationSystem());
                    model.setParent(anchorNode);
                    model.setRenderable( mBPointModel );
                    // .animate(true).start();
                    model.select();
                    if( i==0 ) {
                        lastNode = firstNode = anchorNode;
                    }
                    if( i > 0 ) {
                        addLine(lastNode, anchorNode);
                        lastNode = anchorNode;
                    }
                }
                addLine(lastNode, firstNode);
            }

 */
            // ??????
            for( PICDATA3D pic : picList ) {
                double lat = pic.getCoordy();
                double lon = pic.getCoordx();
                double h = pic.getElevation();
                Anchor anchor = mEarth.createAnchor( lat, lon, h, qx, qy, qz, qw);
                AnchorNode anchorNode = new AnchorNode(anchor);
                anchorNode.setParent(arFragment.getArSceneView().getScene());
                // Create the transformable model and add it to the anchor.
                TransformableNode model = new TransformableNode(arFragment.getTransformationSystem());
                model.setParent(anchorNode);
                Node picNode = createPicNode( model );
                model.select();

                // ???????????????????????????????????????????????????,??????????????????????????????????????????????????? @@
                float az=0;
                GeospatialPose base = mEarth.getCameraGeospatialPose();
                double daz = Math.atan(( lat-base.getLatitude())/(lon-base.getLongitude()));
                az = (float)(daz * 180f / Math.PI ) + 90f;
                // ?????????
                Node titleNode = new Node();
                titleNode.setParent(model);
                titleNode.setEnabled(false);
                titleNode.setLocalPosition(new Vector3(0.0f, 2.0f, 0.0f));
                titleNode.setLocalRotation( Quaternion.axisAngle(new Vector3(0f, 1f, 0f), az));
                titleNode.setRenderable(mViewRenderable);
                titleNode.setEnabled(true);
            }
        }
        // ???GoogleMap ??????????????? Marker
        for( List<Point3> list :ptLists ) {
            if( mMapView != null )
                mMapView.addParcelMarker( list );
        }
        for( PICDATA3D pic : picList ) {
            double lat = pic.getCoordy();
            double lon = pic.getCoordx();
            if( mMapView != null )
                mMapView.addPicMarker( new LatLng(lat, lon) );
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (!GeoPermissionsHelper.hasGeoPermissions(this)) {
            // Use toast instead of snackbar here since the activity will exit.
            Toast.makeText(this, "Camera and location permissions are needed to run this application", Toast.LENGTH_LONG)
                    .show();
            if (!GeoPermissionsHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                GeoPermissionsHelper.launchPermissionSettings(this);
            }
            finish();
        }
    }

    /*
     ????????????????????? anchor
    */
    private void addAnchor( Anchor anchor , LatLng latlng ){
        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setParent(arFragment.getArSceneView().getScene());

        // Create the transformable model and add it to the anchor.
        TransformableNode model = new TransformableNode(arFragment.getTransformationSystem());
        model.setParent(anchorNode);
        model.setRenderable( mPicModel );
               // .animate(true).start();
        model.select();

        // ???????????????????????????????????????????????????,??????????????????????????????????????????????????? @@
        float az=0;
        Earth earth = arFragment.getArSceneView().getSession().getEarth();
        GeospatialPose base;
        if (earth.getTrackingState() == TrackingState.TRACKING ) {
            // TODO: the Earth object may be used here.
            base = earth.getCameraGeospatialPose();
            double daz = Math.atan((latlng.latitude-base.getLatitude())/(latlng.longitude- base.getLongitude()));
            az = (float)(daz * 180f / Math.PI ) + 90f;
        }
        // ?????????
        Node titleNode = new Node();
        titleNode.setParent(model);
        titleNode.setEnabled(false);
        titleNode.setLocalPosition(new Vector3(0.0f, 2.0f, 0.0f));
        titleNode.setLocalRotation( Quaternion.axisAngle(new Vector3(0f, 1f, 0f), az));
        titleNode.setRenderable(mViewRenderable);
        titleNode.setEnabled(true);

            /*
    First, find the vector extending between the two points and define a look rotation
    in terms of this Vector.
*/
        if( mLastAnchor == null)
        {
            mLastAnchor = anchorNode;
            return;
        }
        Vector3 point1 = anchorNode.getWorldPosition();
        Vector3 point2 = mLastAnchor.getWorldPosition();


        final Vector3 difference = Vector3.subtract(point1, point2);
        final Vector3 directionFromTopToBottom = difference.normalized();
        final Quaternion rotationFromAToB =
                Quaternion.lookRotation(directionFromTopToBottom, Vector3.up());
        MaterialFactory.makeOpaqueWithColor(getApplicationContext(), new Color(255, 0, 0))
                .thenAccept(
                        material -> {
                            /* Then, create a rectangular prism, using ShapeFactory.makeCube() and use the difference vector
                                   to extend to the necessary length.  */
                            ModelRenderable lineCube = ShapeFactory.makeCube(
                                    new Vector3(.1f, .1f, difference.length()),
                                    Vector3.zero(), material);
                            /* Last, set the world rotation of the node to the rotation calculated earlier and set the world position to
                                   the midpoint between the given points . */
                            Node node = new Node();
                            node.setParent(anchorNode);
                            node.setRenderable(lineCube);
                            node.setWorldPosition(Vector3.add(point1, point2).scaled(.5f));
                            node.setWorldRotation(rotationFromAToB);
                        }
                );
    }

    private void addLine( AnchorNode a1 , AnchorNode a2 ) {
        Vector3 point1 = a1.getWorldPosition();
        Vector3 point2 = a2.getWorldPosition();
        final Vector3 difference = Vector3.subtract(point1, point2);
        final Vector3 directionFromTopToBottom = difference.normalized();
        final Quaternion rotationFromAToB =
                Quaternion.lookRotation(directionFromTopToBottom, Vector3.up());
        MaterialFactory.makeOpaqueWithColor(getApplicationContext(), new Color(0, 0, 255))
            .thenAccept(
            material -> {
                            /* Then, create a rectangular prism, using ShapeFactory.makeCube() and use the difference vector
                                   to extend to the necessary length.  */
                ModelRenderable lineCube = ShapeFactory.makeCube(
                        new Vector3(.25f, .25f, difference.length()),
                        Vector3.zero(), material);
                            /* Last, set the world rotation of the node to the rotation calculated earlier and set the world position to
                                   the midpoint between the given points . */
                Node node = new Node();
                node.setParent(a2);
                node.setRenderable(lineCube);
                node.setWorldPosition(Vector3.add(point1, point2).scaled(.5f));
                node.setWorldRotation(rotationFromAToB);
            });
    }
    public static double distance( double LatA , double LonA , double LatB , double LonB )
    {
        // ?????????,???????????????,???????????????????????????(?????????????????????,???????????????????????????????????????)
        // ????????????(???)
        double R = 6371004;
        double C = Math.sin(Math.toRadians(LatA)) * Math.sin(Math.toRadians(LatB)) + Math.cos(Math.toRadians(LatA)) * Math.cos(Math.toRadians(LatB)) * Math.cos(Math.toRadians(LonA - LonB));
        return (R * Math.acos(C));
    }

}