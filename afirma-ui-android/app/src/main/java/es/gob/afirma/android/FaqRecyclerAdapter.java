package es.gob.afirma.android;

import android.content.Context;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import es.gob.afirma.R;

public class FaqRecyclerAdapter extends RecyclerView.Adapter<FaqRecyclerAdapter.FaqViewHolder> {

    private final Context context;
    private final List<FaqItem> faqItems;
    private final View navButton;
    private int expandedPosition = -1;

    public FaqRecyclerAdapter(Context context, List<FaqItem> faqItems, View navButton) {
        this.navButton = navButton;
        this.context = context;
        this.faqItems = faqItems;
    }

    @NonNull
    @Override
    public FaqViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_faq, parent, false);
        return new FaqViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FaqViewHolder holder, int position) {
        boolean isExpanded = position == expandedPosition;
        holder.bind(faqItems.get(position), isExpanded);
    }

    @Override
    public int getItemCount() {
        return faqItems.size();
    }

    class FaqViewHolder extends RecyclerView.ViewHolder {

        private final TextView questionTv;
        private final TextView answerTv;
        private final ImageView indicator;

        FaqViewHolder(@NonNull View itemView) {
            super(itemView);
            questionTv = itemView.findViewById(R.id.questionTv);
            answerTv = itemView.findViewById(R.id.answerTv);
            indicator = itemView.findViewById(R.id.indicator);

            answerTv.setVisibility(View.GONE);
            indicator.setRotation(0f);

            itemView.setOnClickListener(v -> toggleExpand());

            itemView.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;

                if (keyCode == KeyEvent.KEYCODE_TAB || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    int position = getBindingAdapterPosition();
                    if (position == RecyclerView.NO_POSITION) return false;

                    boolean isLastItem = position == faqItems.size() - 1;
                    boolean isExpanded = expandedPosition == position;

                    if (isLastItem && isExpanded && answerTv.getVisibility() == View.VISIBLE) {
                        answerTv.requestFocus();
                        answerTv.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
                        return true;
                    }

                    if (isLastItem && !isExpanded && navButton != null) {
                        navButton.requestFocus();
                        navButton.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
                        return true;
                    }
                }

                return false;
            });

            answerTv.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;

                if (keyCode == KeyEvent.KEYCODE_TAB || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    int position = getBindingAdapterPosition();
                    if (position == RecyclerView.NO_POSITION) return false;

                    boolean isLastItem = position == faqItems.size() - 1;

                    if (isLastItem && navButton != null) {
                        navButton.requestFocus();
                        navButton.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
                        return true;
                    }
                }
                return false;
            });
        }

        void bind(FaqItem item, boolean isExpanded) {
            questionTv.setText(item.getQuestion());

            if (item.getAnswers() != null && !item.getAnswers().isEmpty()) {
                answerTv.setText(item.getAnswers().get(0));
            }

            answerTv.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            indicator.setRotation(isExpanded ? 180f : 0f);

            itemView.setContentDescription(item.getQuestion() +
                    (isExpanded ?
                    ", " + context.getString(R.string.faq_deployed) :
                    ", " + context.getString(R.string.faq_collapsed)));
        }

        private void toggleExpand() {
            int position = getBindingAdapterPosition();
            if (position == RecyclerView.NO_POSITION) return;

            boolean isExpanding = expandedPosition != position;
            int oldExpanded = expandedPosition;
            expandedPosition = isExpanding ? position : -1;

            if (oldExpanded >= 0) notifyItemChanged(oldExpanded);
            notifyItemChanged(position);

            if (isExpanding) {
                answerTv.setVisibility(View.VISIBLE);
                answerTv.requestFocus();
                answerTv.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
                answerTv.announceForAccessibility(answerTv.getText());
            }
        }

    }
}
