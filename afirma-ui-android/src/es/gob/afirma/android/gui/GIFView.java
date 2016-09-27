package es.gob.afirma.android.gui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.io.InputStream;

import es.gob.afirma.R;

public class GIFView extends View {

    Movie movie;
    InputStream is = null;
    long moviestart;
    Context cont;

    public GIFView(Context context) {
        super(context);
        init(context);
    }

    public GIFView(Context context, AttributeSet attrs,
                   int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public GIFView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
        this.cont = context;
        Paint p = new Paint();
        p.setAntiAlias(true);
        setLayerType(LAYER_TYPE_SOFTWARE, p);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if(movie != null){
            //setMeasuredDimension(movie.width(), movie.height());//movie.width(), movie.height());
            int height = MeasureSpec.getSize(heightMeasureSpec);
            double porc = 0.9;
            setMeasuredDimension(new Double(height * porc * movie.width() / movie.height() + 0.5d).intValue(), new Double(height * porc + 0.5d).intValue());
        }else
        {
            setMeasuredDimension(getSuggestedMinimumWidth(), getSuggestedMinimumHeight());
        }
    }

    private void init(Context context){
        is=context.getResources().openRawResource(+ R.drawable.dni_nfc);
        movie=Movie.decodeStream(is);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.TRANSPARENT);
        float scaleWidth = ((this.getWidth() / (1f*movie.width())));//add 1f does the trick
        float scaleHeight = ((this.getHeight() / (1f*movie.height())));
        canvas.scale(scaleWidth, scaleHeight);
        movie.draw(canvas, 0, 0);
        super.onDraw(canvas);

        long now=android.os.SystemClock.uptimeMillis();
        if (moviestart == 0) { // first time
            moviestart = now;

        }
        int relTime = (int)((now - moviestart) % movie.duration()) ;
        movie.setTime(relTime);

        invalidate();
    }

}