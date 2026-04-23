package my.edu.utar.RecycleGO;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class QuizFragment extends Fragment {

    private String category;
    private LinearLayout layoutIntro, layoutQuestion, layoutResult;
    private TextView txtQuestionNumber, txtQuestionText, txtScore, txtPointsEarned;
    private Button btnOption1, btnOption2, btnOption3, btnOption4, btnTryAgain;
    private ImageView btnBack, imgQuestion;

    private List<Question> questions = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private int score = 0;

    public QuizFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            category = getArguments().getString("category");
        }
        setupQuestions();
    }

    private void setupQuestions() {
        questions.clear();
        if ("plastic".equals(category)) {
            questions.add(new Question("What is the blue recycling bin used for?", "A. Plastic", "B. Paper", "C. Glass", "D. Food waste", 1));
            questions.add(new Question("Which plastic is commonly used for water bottles?", "A. PET", "B. HDPE", "C. PVC", "D. LDPE", 0));
            questions.add(new Question("What does the number 2 inside a recycling triangle represent?", "A. PET", "B. HDPE", "C. PP", "D. PS", 1));
        } else {
            questions.add(new Question("Which of these can be composted?", "A. Meat", "B. Plastic bags", "C. Vegetable scraps", "D. Metal foil", 2));
            questions.add(new Question("What is the main benefit of composting food waste?", "A. Produces energy", "B. Creates fertilizer", "C. Reduces plastic", "D. Kills pests", 1));
            questions.add(new Question("Is citrus peel good for all types of composting?", "A. Yes", "B. No, too acidic", "C. Only if dried", "D. Only if boiled", 1));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quiz, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        layoutIntro = view.findViewById(R.id.layout_intro);
        layoutQuestion = view.findViewById(R.id.layout_question);
        layoutResult = view.findViewById(R.id.layout_result);

        txtQuestionNumber = view.findViewById(R.id.txt_question_number);
        txtQuestionText = view.findViewById(R.id.txt_question_text);
        txtScore = view.findViewById(R.id.txt_score);
        txtPointsEarned = view.findViewById(R.id.txt_points_earned);
        imgQuestion = view.findViewById(R.id.img_question);

        btnOption1 = view.findViewById(R.id.btn_option1);
        btnOption2 = view.findViewById(R.id.btn_option2);
        btnOption3 = view.findViewById(R.id.btn_option3);
        btnOption4 = view.findViewById(R.id.btn_option4);
        btnBack = view.findViewById(R.id.btn_back);
        btnTryAgain = view.findViewById(R.id.btn_try_again);

        TextView title = view.findViewById(R.id.quiz_title);
        TextView description = view.findViewById(R.id.quiz_description);

        if ("plastic".equals(category)) {
            title.setText("Plastic Recycling Quiz");
            description.setText("Test your knowledge about plastic recycling categories!");
        } else {
            title.setText("Food Waste Quiz");
            description.setText("Learn how to manage food waste effectively!");
        }

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        view.findViewById(R.id.btn_start_quiz).setOnClickListener(v -> startQuiz());
        view.findViewById(R.id.btn_finish).setOnClickListener(v -> getParentFragmentManager().popBackStack());
        btnTryAgain.setOnClickListener(v -> resetQuiz());

        btnOption1.setOnClickListener(v -> checkAnswer(0));
        btnOption2.setOnClickListener(v -> checkAnswer(1));
        btnOption3.setOnClickListener(v -> checkAnswer(2));
        btnOption4.setOnClickListener(v -> checkAnswer(3));
    }

    private void startQuiz() {
        layoutIntro.setVisibility(View.GONE);
        layoutResult.setVisibility(View.GONE);
        layoutQuestion.setVisibility(View.VISIBLE);
        showQuestion();
    }

    private void resetQuiz() {
        currentQuestionIndex = 0;
        score = 0;
        startQuiz();
    }

    private void showQuestion() {
        Button[] buttons = {btnOption1, btnOption2, btnOption3, btnOption4};
        for (Button btn : buttons) {
            btn.setEnabled(true);
            btn.setBackgroundResource(R.drawable.quiz_option_default_bg);
            btn.setBackgroundTintList(null); 
            btn.setTextColor(android.graphics.Color.BLACK); 
        }

        Question q = questions.get(currentQuestionIndex);
        txtQuestionNumber.setText("Question " + (currentQuestionIndex + 1));
        txtQuestionText.setText(q.question);
        btnOption1.setText(q.options[0]);
        btnOption2.setText(q.options[1]);
        btnOption3.setText(q.options[2]);
        btnOption4.setText(q.options[3]);
    }

    private void checkAnswer(int selectedIndex) {
        int correctIndex = questions.get(currentQuestionIndex).correctAnswerIndex;
        Button[] buttons = {btnOption1, btnOption2, btnOption3, btnOption4};

        for (Button btn : buttons) {
            btn.setEnabled(false);
        }

        if (selectedIndex == correctIndex) {
            score++;
            buttons[selectedIndex].setBackgroundResource(R.drawable.quiz_option_correct_bg);
            buttons[selectedIndex].setBackgroundTintList(null);
            buttons[selectedIndex].setTextColor(android.graphics.Color.WHITE); 
        } else {
            // Only turn the selected WRONG button red. 
            // Do NOT turn the correct button green anymore.
            buttons[selectedIndex].setBackgroundResource(R.drawable.quiz_option_wrong_bg);
            buttons[selectedIndex].setBackgroundTintList(null);
            buttons[selectedIndex].setTextColor(android.graphics.Color.WHITE); 
        }

        new Handler().postDelayed(() -> {
            if (!isAdded()) return;
            currentQuestionIndex++;
            if (currentQuestionIndex < questions.size()) {
                showQuestion();
            } else {
                showResult();
            }
        }, 1500);
    }

    private void showResult() {
        layoutQuestion.setVisibility(View.GONE);
        layoutResult.setVisibility(View.VISIBLE);
        
        int totalPoints = score * 10;
        txtPointsEarned.setText("+" + totalPoints);
        txtScore.setText("Score: " + score + "/" + questions.size());

        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        int currentPoints = prefs.getInt("totalPoints", 1000);
        prefs.edit().putInt("totalPoints", currentPoints + totalPoints).apply();
    }

    private static class Question {
        String question;
        String[] options;
        int correctAnswerIndex;

        Question(String q, String o1, String o2, String o3, String o4, int correct) {
            this.question = q;
            this.options = new String[]{o1, o2, o3, o4};
            this.correctAnswerIndex = correct;
        }
    }
}
