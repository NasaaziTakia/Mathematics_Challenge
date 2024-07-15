<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use App\Models\Challenge;
use App\Models\Questions;
use App\Models\Answers;

class ChallengeController extends Controller
{
    public function showQuestions($id)
    {
        // Get the challenge based on the provided id
        $challenge = Challenge::findOrFail($id);

        // Get questions for the challenge
        $questions = Questions::where('challenge_id', $id)->get();

        // Prepare an array to store questions and their answers
        $questionsWithAnswers = [];

        foreach ($questions as $question) {
            // Find corresponding answer for each question
            $answer = Answers::where('question_id', $question->id)->first();

            // Push question and answer to the array
            $questionsWithAnswers[] = [
                'question' => $question->question_text,
                'answer' => $answer ? $answer->answer_text : 'No answer found',
            ];
        }

        // Pass data to the view
        return view('questions', [
            'challenge' => $challenge,
            'questionsWithAnswers' => $questionsWithAnswers,
        ]);
    }
}
