package es.gob.afirma.android;

import java.util.List;

public class FaqItem {
    public String question;
    public List<CharSequence> answers;

    public FaqItem(String question, List<CharSequence> answers) {
        this.question = question;
        this.answers = answers;
    }

    public String getQuestion() {
        return question;
    }

    public List<CharSequence> getAnswers() {
        return answers;
    }
}
