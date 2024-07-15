<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use App\Imports\QuestionsImport;
use App\Imports\AnswersImport;
use App\Models\Answer;
use App\Models\Answers;
use App\Models\Challenge;
use App\Models\Question;
use App\Models\Questions;
use Maatwebsite\Excel\Facades\Excel;

class ImportController extends Controller
{
    /**
     * Import the Excel files and store the data.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return \Illuminate\Http\RedirectResponse
     */
    public function import(Request $request)
    {
        // Validate incoming request data
        $request->validate([
            'questionsFile' => 'required|mimes:xlsx,xls',
            'answersFile' => 'required|mimes:xlsx,xls',
            'challengeNumber' => 'required',
            'startDate' => 'required|date',
            'endDate' => 'required|date|after_or_equal:startDate',
            'duration' => 'required|integer|min:1',
            'questionsPerAttempt' => 'required|integer|min:1',
        ]);

        try {
            // Import Excel files
            $questionsImport = new QuestionsImport;
            $answersImport = new AnswersImport;

            Excel::import($questionsImport, $request->file('questionsFile'));
            Excel::import($answersImport, $request->file('answersFile'));

            // Insert Challenge data
            $challenge = new Challenge;
            $challenge->challengeNumber = $request->challengeNumber;
            $challenge->start_date = $request->startDate;
            $challenge->end_date = $request->endDate;
            $challenge->duration = $request->duration;
            $challenge->num_questions = $request->questionsPerAttempt;
            $challenge->save();

            // Retrieve the ID of the inserted challenge
            $challengeId = $challenge->id;

            // Insert Questions and Answers data
            $questionsData = $questionsImport->data;
            $answersData = $answersImport->data;

            foreach ($questionsData as $questionRow) {
                $question = new Questions;
                $question->question_text = $questionRow['question']; // Adjust according to your column names
                $question->marks = $questionRow['marks']; // Adjust according to your column names
                $question->challenge_id = $challengeId;
                $question->save();

                // Find corresponding answer from answers data
                $answerText = $this->findAnswerByQuestionNumber($questionRow['number'], $answersData);

                if ($answerText !== null) {
                    $answer = new Answers;
                    $answer->question_id = $question->id;
                    $answer->answer_text = $answerText; // Adjust according to your column names
                    $answer->save();
                }
            }

            session()->flash('success', 'Data imported successfully.');
            return view('home');
        } catch (\Exception $e) {
            // Log the error for debugging purposes
            //Log::error('Error during data import: ' . $e->getMessage());


            // Flash an error message to the session
            session()->flash('error', 'An error occurred while importing data. Please try again.');
            dd($e);
            // Redirect back to the import form with old input
            // return redirect()->back()->withInput();
        }
    }

    /**
     * Find answer by question number from imported answers data.
     *
     * @param string $questionNumber
     * @param array $answersData
     * @return string|null
     */
    private function findAnswerByQuestionNumber($questionNumber, $answersData)
    {
        //dd($answersData);
        foreach ($answersData as $answerRow) {

            if ($answerRow['question_number'] == $questionNumber) {
                return $answerRow['answer']; // Adjust according to your column names
            }
        }
        return null;
    }
}
