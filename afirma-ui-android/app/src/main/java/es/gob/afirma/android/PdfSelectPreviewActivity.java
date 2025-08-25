package es.gob.afirma.android;

import static es.gob.afirma.android.LocalSignActivity.ERROR_REQUEST_VISIBLE_SIGN;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.aowagie.text.Rectangle;
import com.aowagie.text.pdf.PdfReader;
import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnDrawListener;
import com.github.barteksc.pdfviewer.listener.OnErrorListener;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnRenderListener;
import com.github.barteksc.pdfviewer.util.FitPolicy;
import com.google.android.material.appbar.MaterialToolbar;
import com.shockwave.pdfium.PdfPasswordException;
import com.shockwave.pdfium.util.SizeF;

import java.io.File;
import java.io.IOException;

import es.gob.afirma.R;
import es.gob.afirma.android.gui.CustomDialog;
import es.gob.afirma.android.gui.PDFPasswordVisibleSignDialog;
import es.gob.afirma.android.util.FileUtil;
import es.gob.afirma.android.util.Utils;
import es.gob.afirma.signers.pades.common.PdfExtraParams;

public class PdfSelectPreviewActivity extends AppCompatActivity {

    private Context context;

    File file;
    PDFView pdfView;
    private float startX, startY, endX, endY;
    private int pageNumber = 0;
    private int totalPages;
    private String pdfPassword = "";
    private boolean isFirstPwdRequest;

    private Paint paint;
    private RectF selectedArea;

    Button prevPageBtn;
    Button nextPageBtn;
    TextView pageNumberTv;
    ProgressBar progressBar;

    private static final int LOWER_LIMIT_X = 30;
    private static final int LOWER_LIMIT_Y = 45;
    private static final int AREA_LIMIT = 1350;

    boolean isHorizontal;
    boolean isRequiredVisibleSignature;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdfview);

        MaterialToolbar toolbar = findViewById(R.id.pdfViewToolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        this.context = this;
        this.isFirstPwdRequest = true;

        prevPageBtn = findViewById(R.id.prevPageBtn);
        nextPageBtn = findViewById(R.id.nextPageBtn);
        pageNumberTv = findViewById(R.id.pageNumberTv);
        progressBar = findViewById(R.id.loadingPdfBar);

        pdfView = findViewById(R.id.pdfView);
        String filePath = getIntent().getStringExtra("filePath");
        this.isRequiredVisibleSignature = getIntent().getBooleanExtra("isRequiredVisibleSignature", false);
        file = new File(filePath);

        paint = new Paint();
        paint.setColor(Color.parseColor("#80000000"));
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStrokeWidth(5);

        Button nextButton = findViewById(R.id.nextButton);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent dataIntent = new Intent();

                dataIntent.putExtra(PdfExtraParams.SIGNATURE_PAGES, String.valueOf(pageNumber + 1));
                if (pdfPassword != null && !pdfPassword.isEmpty()) {
                    dataIntent.putExtra(PdfExtraParams.OWNER_PASSWORD_STRING,
                            pdfPassword.getBytes()
                    );
                }

                SizeF pageSize = pdfView.getPageSize(pageNumber);
                float zoom = pdfView.getZoom();
                float pageWidth = pageSize.getWidth() * zoom;
                float pageHeight = pageSize.getHeight() * zoom;

                float xOffset = pdfView.getCurrentXOffset();

                if (isHorizontal) {
                    startX = startX + xOffset;
                    endX = endX + xOffset;
                }

                if (selectedArea == null) {
                    if (isRequiredVisibleSignature) {
                        CustomDialog signFragmentCustomDialog = new CustomDialog(context, R.drawable.baseline_info_24, getString(R.string.not_selected_area),
                                getString(R.string.not_selected_area_desc), getString(R.string.reselect_area), true, getString(R.string.cancel));
                        signFragmentCustomDialog.setAcceptButtonClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                signFragmentCustomDialog.cancel();
                            }
                        });
                        signFragmentCustomDialog.setCancelButtonClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                signFragmentCustomDialog.cancel();
                                setResult(Activity.RESULT_CANCELED, dataIntent);
                                finish();
                            }
                        });
                        signFragmentCustomDialog.show();
                        return;
                    } else {
                        CustomDialog signFragmentCustomDialog = new CustomDialog(context, R.drawable.baseline_info_24, getString(R.string.not_selected_area),
                                getString(R.string.not_selected_area_desc), getString(R.string.drag_on), true, getString(R.string.reselect_area));
                        signFragmentCustomDialog.setAcceptButtonClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                signFragmentCustomDialog.cancel();
                                setResult(Activity.RESULT_OK, dataIntent);
                                finish();
                            }
                        });
                        signFragmentCustomDialog.setCancelButtonClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                signFragmentCustomDialog.cancel();
                            }
                        });
                        signFragmentCustomDialog.show();
                        return;
                    }
                }

                final int areaHeight = (int) (selectedArea.height() + startY > pageHeight ?
                                        pageHeight - startY : selectedArea.height());
                final int areaWidth = (int) (selectedArea.width() + startX > pageWidth ?
                                        pageWidth - startX : selectedArea.width());

                float pdfHeight = 0;
                float pdfWidth = 0;

                try {
                    byte [] fileContent = FileUtil.readDataFromFile(file);
                    PdfReader reader;
                    if (pdfPassword != null && !pdfPassword.isEmpty()) {
                        reader = new PdfReader(fileContent, pdfPassword.getBytes());
                    } else {
                       reader = new PdfReader(fileContent);
                    }
                    Rectangle rect = reader.getPageSize(pageNumber + 1);
                    pdfHeight = rect.getHeight();
                    pdfWidth = rect.getWidth();
                }
                catch (OutOfMemoryError oome) {
                    final Intent resultIntent = new Intent();
                    setResult(ERROR_REQUEST_VISIBLE_SIGN, resultIntent);
                    finish();
                }
                catch (Exception e) {
                    final Intent resultIntent = new Intent();
                    setResult(ERROR_REQUEST_VISIBLE_SIGN, resultIntent);
                    finish();
                }

                float lowerLeftX = (startX * pdfWidth) / pageWidth;
                float upperRightX = ((startX + areaWidth) * pdfWidth) / pageWidth;
                float lowerLeftY = ((pageHeight - areaHeight - startY) * pdfHeight) / pageHeight;
                float upperRightY = ((pageHeight - startY) * pdfHeight) / pageHeight;

                final int dimensionX = Math.abs((int)(upperRightX - lowerLeftX));
                final int dimensionY = Math.abs((int)(upperRightY - lowerLeftY));

                final boolean limitSizeCondition = dimensionX < LOWER_LIMIT_X || dimensionY < LOWER_LIMIT_Y;
                final boolean limitAreaCondition = dimensionX * dimensionY < AREA_LIMIT;

                if (limitSizeCondition || limitAreaCondition) {
                    if (isRequiredVisibleSignature) {
                        CustomDialog signFragmentCustomDialog = new CustomDialog(context, R.drawable.baseline_info_24, getString(R.string.visible_sign_small),
                                getString(R.string.small_area_warning), getString(R.string.reselect_area), true, getString(R.string.cancel));
                        signFragmentCustomDialog.setAcceptButtonClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                signFragmentCustomDialog.cancel();
                            }
                        });
                        signFragmentCustomDialog.setCancelButtonClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                signFragmentCustomDialog.cancel();
                                setResult(Activity.RESULT_CANCELED, dataIntent);
                                finish();
                            }
                        });
                        signFragmentCustomDialog.show();
                        return;
                    } else {
                        CustomDialog signFragmentCustomDialog = new CustomDialog(context, R.drawable.baseline_info_24, getString(R.string.visible_sign_small),
                                getString(R.string.small_area_warning), getString(R.string.drag_on), true, getString(R.string.reselect_area));
                        signFragmentCustomDialog.setAcceptButtonClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                signFragmentCustomDialog.cancel();
                                setResult(Activity.RESULT_OK, dataIntent);
                                finish();
                            }
                        });
                        signFragmentCustomDialog.setCancelButtonClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                signFragmentCustomDialog.cancel();
                            }
                        });
                        signFragmentCustomDialog.show();
                        return;
                    }
                }

                dataIntent.putExtra(PdfExtraParams.SIGNATURE_POSITION_ON_PAGE_LOWER_LEFTX,
                        Integer.toString(
                                Math.round(
                                        lowerLeftX * zoom
                                )
                        )
                );
                dataIntent.putExtra(PdfExtraParams.SIGNATURE_POSITION_ON_PAGE_LOWER_LEFTY,
                        Integer.toString(
                                Math.round(
                                        lowerLeftY * zoom
                                )
                        )
                );
                dataIntent.putExtra(PdfExtraParams.SIGNATURE_POSITION_ON_PAGE_UPPER_RIGHTX,
                        Integer.toString(
                                Math.round(
                                        upperRightX * zoom
                                )
                        )
                );
                dataIntent.putExtra(PdfExtraParams.SIGNATURE_POSITION_ON_PAGE_UPPER_RIGHTY,
                        Integer.toString(
                                Math.round(
                                        upperRightY * zoom
                                )
                        )
                );

                setResult(Activity.RESULT_OK, dataIntent);
                finish();
            }
        });

        try{
            openPDF(this);
        } catch (IOException ioe){
            final Intent dataIntent = new Intent();
            setResult(ERROR_REQUEST_VISIBLE_SIGN, dataIntent);
            finish();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    public void openPDF(PdfSelectPreviewActivity parentActivity) throws IOException {

        pdfView.fromFile(file)
                .defaultPage(0)
                .enableAnnotationRendering(true)
                .pageFitPolicy(FitPolicy.BOTH)
                .fitEachPage(true)
                .password(pdfPassword)
                .onLoad(new OnLoadCompleteListener() {
                    @Override
                    public void loadComplete(int numberOfPages) {
                        totalPages = numberOfPages;
                        adjustPdfView(parentActivity);
                    }
                })
                .onDraw(new OnDrawListener() {
                    @Override
                    public void onLayerDrawn(Canvas canvas, float pageWidth, float pageHeight, int displayedPage) {
                        drawSelectedArea(canvas);
                    }
                })
                .onError(new OnErrorListener() {
                    @Override
                    public void onError(Throwable t) {
                        if (t instanceof PdfPasswordException) {
                            PDFPasswordVisibleSignDialog pwdDialog = new PDFPasswordVisibleSignDialog(parentActivity, !isFirstPwdRequest);
                            pwdDialog.show();
                            isFirstPwdRequest = false;
                        }
                    }
                })
                .load();

        pdfView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                float xOffset = pdfView.getCurrentXOffset();
                float zoom = pdfView.getZoom();

                // Verifica si el visor está en modo horizontal o vertical
                isHorizontal = pdfView.getWidth() > pdfView.getHeight();

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (isHorizontal) {
                            startX = (event.getX() - xOffset) / zoom;
                        } else {
                            startX = event.getX() / zoom; // Sin ajuste si es vertical
                        }
                        startY = event.getY() / zoom;
                        break;

                    case MotionEvent.ACTION_MOVE:
                        if (isHorizontal) {
                            endX = (event.getX() - xOffset) / zoom;
                        } else {
                            endX = event.getX() / zoom;
                        }
                        endY = event.getY() / zoom;
                        selectedArea = createClampedSelection(startX, startY, endX, endY);
                        pdfView.invalidate();
                        break;

                    case MotionEvent.ACTION_UP:
                        if (isHorizontal) {
                            endX = (event.getX() - xOffset) / zoom;
                        } else {
                            endX = event.getX() / zoom;
                        }
                        endY = event.getY() / zoom;
                        // En caso de que se haya dibujado el area en sentido contrario, se recalculan las coordenadas
                        if (startX > endX) {
                            float startXAux = startX;
                            startX = endX;
                            endX = startXAux;
                        }
                        if (startY > endY) {
                            float startYAux = startY;
                            startY = endY;
                            endY = startYAux;
                        }
                        pdfView.invalidate();
                        break;
                }
                return true;
            }
        });

        totalPages = pdfView.getPageCount();

        Button firstPageBtn = findViewById(R.id.firstPageBtn);
        firstPageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (pageNumber > 0) {
                    pageNumber = 0;
                    onPageChanged(parentActivity);
                }
            }
        });

        Button nextPageBtn = findViewById(R.id.nextPageBtn);
        nextPageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (pageNumber < totalPages -1) {
                    pageNumber++;
                    onPageChanged(parentActivity);
                }
            }
        });

        Button prevPageBtn = findViewById(R.id.prevPageBtn);
        prevPageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (pageNumber > 0) {
                    pageNumber--;
                    onPageChanged(parentActivity);
                }
            }
        });

        Button lastPageBtn = findViewById(R.id.lastPageBtn);
        lastPageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (pageNumber < totalPages - 1) {
                    pageNumber = totalPages - 1;
                    onPageChanged(parentActivity);
                }
            }
        });

    }

    // Método para clonar la selección dentro de los límites de la página
    private RectF createClampedSelection(float startX, float startY, float endX, float endY) {
        if (startX > endX) {
            float startXAux = startX;
            startX = endX;
            endX = startXAux;
        }
        if (startY > endY) {
            float startYAux = startY;
            startY = endY;
            endY = startYAux;
        }
        return new RectF(
                startX,  // X mínimo
                startY,  // Y mínimo
                endX,  // X máximo
                endY   // Y máximo
        );
    }

    public void onPageChanged(PdfSelectPreviewActivity parentActivity) {
        disableButtons();
        progressBar.setVisibility(View.VISIBLE);
        selectedArea = null;
        adjustPdfView(parentActivity);
        pdfView.invalidate();
    }

    // Método para dibujar el área seleccionada dentro del PDF
    private void drawSelectedArea(Canvas canvas) {
        // Dibuja el área seleccionada si existe
        if (selectedArea != null) {
            canvas.drawRect(selectedArea, paint);
        }
    }

    private void adjustPdfView(PdfSelectPreviewActivity parentActivity) {
        SizeF pageSize = pdfView.getPageSize(pageNumber);
        if (pageSize != null) {
            float zoom = pdfView.getZoom();

            float pageWidth = pageSize.getWidth() * zoom;
            float pageHeight = pageSize.getHeight() * zoom;

            pdfView.getLayoutParams().width = (int) pageWidth;
            pdfView.getLayoutParams().height = (int) pageHeight;
            pdfView.requestLayout();

            FitPolicy fitPolicy;
             if (pageWidth > pageHeight) {
                fitPolicy = FitPolicy.WIDTH;
            } else {
                fitPolicy = FitPolicy.BOTH;
            }

            pdfView.fromFile(file)
                    .defaultPage(pageNumber)
                    .enableAnnotationRendering(true)
                    .pageFitPolicy(fitPolicy)
                    .fitEachPage(true)
                    .password(pdfPassword)
                    .autoSpacing(false)
                    .onRender(new OnRenderListener() {
                        @Override
                        public void onInitiallyRendered(int nbPages) {
                            enableButtons();
                            progressBar.setVisibility(View.INVISIBLE);
                            pageNumberTv.setText(getString(R.string.page_number, String.valueOf(pageNumber + 1), String.valueOf(totalPages)));
                            pdfView.invalidate();
                        }
                    })
                    .onDraw(new OnDrawListener() {
                        @Override
                        public void onLayerDrawn(Canvas canvas, float pageWidth, float pageHeight, int displayedPage) {
                            drawSelectedArea(canvas);
                        }
                    })
                    .onError(new OnErrorListener() {
                        @Override
                        public void onError(Throwable t) {
                            if (t instanceof PdfPasswordException) {
                                PDFPasswordVisibleSignDialog pwdDialog = new PDFPasswordVisibleSignDialog(parentActivity, !isFirstPwdRequest);
                                pwdDialog.show();
                                isFirstPwdRequest = false;
                            }
                        }
                    })
                    .load();
        }
    }

    private void enableButtons() {
        prevPageBtn.setEnabled(true);
        nextPageBtn.setEnabled(true);
    }

    private void disableButtons() {
        prevPageBtn.setEnabled(false);
        nextPageBtn.setEnabled(false);
    }

    public void setPdfPassword(String pwd) {
        this.pdfPassword = pwd;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

}
