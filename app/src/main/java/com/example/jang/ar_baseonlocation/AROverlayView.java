package com.example.jang.ar_baseonlocation;





import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.location.Location;
import android.opengl.Matrix;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import com.example.jang.ar_baseonlocation.LocationHelper;
import com.example.jang.ar_baseonlocation.ARPoint;
import com.google.android.gms.maps.GoogleMap;

import static com.example.jang.ar_baseonlocation.ARActivity.TAG;

/**
 * Created by ntdat on 1/13/17.
 */

public class AROverlayView extends View {
    private GoogleMap mMap;
    Context context;
    private float[] rotatedProjectionMatrix = new float[16];
    private Location currentLocation;
    public List<ARPoint> arPoints;
    public int select = 0;




    public AROverlayView(Context context) {
        super(context);

        this.context = context;

      /* //Demo points
        arPoints = new ArrayList<ARPoint>() {{
            add(new ARPoint("하안사거리", 37.461701, 126.880365, 0));

            add(new ARPoint("독산역", 37.466077, 126.889625, 0));
        }};*/
    }

    public void updateRotatedProjectionMatrix(float[] rotatedProjectionMatrix) {
        this.rotatedProjectionMatrix = rotatedProjectionMatrix;
        this.invalidate();
    }

    public void updateCurrentLocation(Location currentLocation){
        this.currentLocation = currentLocation;
        this.invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);


        if (currentLocation == null) {
            return;
        }

        final int radius = 30;
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setTextSize(60);

        Paint myPaint = new Paint();
        myPaint.setAntiAlias(true);
        myPaint.setColor(Color.BLACK);
        myPaint.setStrokeWidth(10f);


        for (int i = 0; i < arPoints.size(); i ++) {
            float[] currentLocationInECEF = LocationHelper.WSG84toECEF(currentLocation);
            float[] pointInECEF = LocationHelper.WSG84toECEF(arPoints.get(i).getLocation());
            float[] pointInENU = LocationHelper.ECEFtoENU(currentLocation, currentLocationInECEF, pointInECEF);

            float[] cameraCoordinateVector = new float[4];
            Matrix.multiplyMV(cameraCoordinateVector, 0, rotatedProjectionMatrix, 0, pointInENU, 0);


            float x2 = 0;
            float y2 = 0;
            if(i+1 != arPoints.size()) {
                float[] currentLocationInECEF2 = LocationHelper.WSG84toECEF(currentLocation);
                float[] pointInECEF2 = LocationHelper.WSG84toECEF(arPoints.get(i + 1).getLocation());
                float[] pointInENU2 = LocationHelper.ECEFtoENU(currentLocation, currentLocationInECEF2, pointInECEF2);

                float[] cameraCoordinateVector2 = new float[4];
                Matrix.multiplyMV(cameraCoordinateVector2, 0, rotatedProjectionMatrix, 0, pointInENU2, 0);

                 x2  = (0.5f + cameraCoordinateVector2[0]/cameraCoordinateVector2[3]) * canvas.getWidth();
                 y2 = (0.5f - cameraCoordinateVector2[1]/cameraCoordinateVector2[3]) * canvas.getHeight();
            }

            // cameraCoordinateVector[2] is z, that always less than 0 to display on right position
            // if z > 0, the point will display on the opposite
            if (cameraCoordinateVector[2] < 0) {
                float x  = (0.5f + cameraCoordinateVector[0]/cameraCoordinateVector[3]) * canvas.getWidth();
                float y = (0.5f - cameraCoordinateVector[1]/cameraCoordinateVector[3]) * canvas.getHeight();
                //Log.d("arPoints Orin",arPoints.get(i).name +":"+arPoints.get(i).location.toString());



                canvas.drawCircle(x, y, radius, paint);
                if(x2 != 0 && y2 != 0) {
                    canvas.drawLine(x, y, x2, y2, myPaint);
                }
                //Log.d("x", String.valueOf(x)+":"+String.valueOf(i));
                //Log.d("y", String.valueOf(y)+":"+String.valueOf(i));
                canvas.drawText(arPoints.get(i).getName(), x - (30 * arPoints.get(i).getName().length() / 2), y - 80, paint);


            }
        }

/*

        float[] currentLocationInECEF = LocationHelper.WSG84toECEF(currentLocation);
        float[] pointInECEF = LocationHelper.WSG84toECEF(arPoints.get(select).getLocation());
        float[] pointInENU = LocationHelper.ECEFtoENU(currentLocation, currentLocationInECEF, pointInECEF);

        float[] cameraCoordinateVector = new float[4];
        Matrix.multiplyMV(cameraCoordinateVector, 0, rotatedProjectionMatrix, 0, pointInENU, 0);

        // cameraCoordinateVector[2] is z, that always less than 0 to display on right position
        // if z > 0, the point will display on the opposite
        if (cameraCoordinateVector[2] < 0) {
            float x = (0.5f + cameraCoordinateVector[0] / cameraCoordinateVector[3]) * canvas.getWidth();
            float y = (0.5f - cameraCoordinateVector[1] / cameraCoordinateVector[3]) * canvas.getHeight();
            //Log.d("arPoints Orin",arPoints.get(i).name +":"+arPoints.get(i).location.toString());
            canvas.drawCircle(x, y, radius, paint);
            //Log.d("x", String.valueOf(x)+":"+String.valueOf(i));
            //Log.d("y", String.valueOf(y)+":"+String.valueOf(i));
            canvas.drawText(arPoints.get(select).getName(), x - (30 * arPoints.get(select).getName().length() / 2), y - 80, paint);


        }
*/
    }

}
