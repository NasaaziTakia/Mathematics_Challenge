<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;

use App\Models\School;

use App\Import;

use App\Models\Question;

use App\Models\Participants;

use App\Models\Rejected;

use App\Models\Attempt;

use App\Models\Challenge;

use App\Models\Answer;

use App\Models\Representative;

use Maatwebsite\Excel\Facades\Excel;

use App\Imports\QuestionsImport;

use App\Imports\AnswersImport;

class AdminController extends Controller
{

public function addview(){
    return view('admin.add_schools');
}

public function addschools(Request $request){
    $school =new school;

    $school->name=$request->name;
    $school->district=$request->district;
    $school->registration_number=$request->registration_number;
    $school->representative_name=$request->representative_name;
    $school->representative_email=$request->representative_email;

    $school ->save();

    return redirect()->back()->with('message', 'School added successfully!');

}

public function participantview(){
    $data =participants::all();
    return view('admin.participant', compact('data'));
}

public function add_challenges()
{
    return view('admin.challenges');
   }
public function representative_view(){
    $data1 =reprsentative::all();
    return view('admin.representative', compact('data1'));
}

//Rejected participants function
public function viewrejected(){
    $data =rejected::all();
    return view('admin.rejected', compact('data'));
   }

//View reports
public function viewreport(){


    return view('admin.report');
}


//View attempt
public function viewattempt(){

    // Retrieve all attempts from the database
    $attempts = Attempt::all();

    // Pass the attempts to the Blade view
    return view('admin.attempt', compact('attempts'));
}
}



