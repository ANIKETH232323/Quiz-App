package com.example.quizapp;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class AddQuestionActivity extends AppCompatActivity {

    private EditText question;
    private RadioGroup options;
    private LinearLayout answers;
    private int position;
    private Dialog loadingDialog;
    private TextView loadingText;
    private QuestionModel questionModel;
    private String id, setId;

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_question);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Objects.requireNonNull(getSupportActionBar()).setTitle("Add Question");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        loadingDialog = new Dialog(this);
        loadingDialog.setContentView(R.layout.loading);
        loadingDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.rounded_corners));
        loadingDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        loadingDialog.setCancelable(false);
        loadingText = loadingDialog.findViewById(R.id.loadingText);

        getIntent().getStringExtra("categoryName");
        setId = getIntent().getStringExtra("setId");
        position = getIntent().getIntExtra("position", -1);
        question = findViewById(R.id.question);
        options = findViewById(R.id.options);
        answers = findViewById(R.id.answers);
        Button uploadBtn = findViewById(R.id.button);

        if (setId == null) {
            finish();
            return;
        }

        if (position != -1) {
            questionModel = QuestionsActivity.list.get(position);
            setData();
        }

        uploadBtn.setOnClickListener(view -> {
            if (question.getText().toString().isEmpty()) {
                question.setError("Required");
                return;
            }
            upload();
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    private void setData() {

        question.setText(questionModel.getQuestion());

        ((EditText)answers.getChildAt(0)).setText(questionModel.getA());
        ((EditText)answers.getChildAt(1)).setText(questionModel.getB());
        ((EditText)answers.getChildAt(2)).setText(questionModel.getC());
        ((EditText)answers.getChildAt(3)).setText(questionModel.getD());

        for (int i = 0; i < answers.getChildCount(); i++) {
            if (((EditText)answers.getChildAt(i)).getText().toString().equals(questionModel.getAnswer())) {
                RadioButton radioButton = (RadioButton) options.getChildAt(i);
                radioButton.setChecked(true);
                break;
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private void upload() {

        int correct = -1;
        for (int i = 0; i < options.getChildCount(); i++) {

            EditText answer = (EditText) answers.getChildAt(i);
            if (answer.getText().toString().isEmpty()) {
                answer.setError("Required");
                return;
            }

            RadioButton radioButton = (RadioButton) options.getChildAt(i);
            if (radioButton.isChecked()) {
                correct = i;
                break;
            }
        }

        if (correct == -1) {
            Toast.makeText(this, "Error AQ134: Please mark the correct answer", Toast.LENGTH_SHORT).show();
            return;
        }

        final HashMap<String,Object> map = new HashMap<>();
        map.put("correctAns", ((EditText)answers.getChildAt(correct)).getText().toString());
        map.put("optionA", ((EditText)answers.getChildAt(0)).getText().toString());
        map.put("optionB", ((EditText)answers.getChildAt(1)).getText().toString());
        map.put("optionC", ((EditText)answers.getChildAt(2)).getText().toString());
        map.put("optionD", ((EditText)answers.getChildAt(3)).getText().toString());
        map.put("question", question.getText().toString());
        map.put("setId", setId);

        if (position != -1) {
            id = questionModel.getId();
            loadingText.setText("Updating question...");
        }else {
            id = UUID.randomUUID().toString();
            loadingText.setText("Adding question...");
        }

        loadingDialog.show();

        FirebaseDatabase.getInstance().getReference()
                .child("SETS").child(setId).child(id)
                .setValue(map).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {

                        QuestionModel questionModel = new QuestionModel(Objects.requireNonNull(map.get("question")).toString()
                                , Objects.requireNonNull(map.get("optionA")).toString()
                                , Objects.requireNonNull(map.get("optionB")).toString()
                                , Objects.requireNonNull(map.get("optionC")).toString()
                                , Objects.requireNonNull(map.get("optionD")).toString()
                                , Objects.requireNonNull(map.get("correctAns")).toString()
                                , id
                                , Objects.requireNonNull(map.get("setId")).toString());

                        if (position != -1) {
                            QuestionsActivity.list.set(position, questionModel);
                        } else {
                            QuestionsActivity.list.add(questionModel);
                        }

                        finish();
                    } else {
                        Toast.makeText(AddQuestionActivity.this, "Error AQ181: Something went wrong", Toast.LENGTH_SHORT).show();
                    }
                    loadingDialog.dismiss();
                    loadingText.setText("Loading...");
                });
    }
}