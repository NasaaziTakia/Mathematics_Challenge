<?php

use Illuminate\Support\Facades\Route;
use App\Models\Participants; // Import the User model
use App\Http\Controllers\ImportController;
use App\Http\Controllers\ChallengeController;
use App\Models\Challenge;

/*
|--------------------------------------------------------------------------
| Web Routes
|--------------------------------------------------------------------------
|
| Here is where you can register web routes for your application. These
| routes are loaded by the RouteServiceProvider within a group which
| contains the "web" middleware group. Now create something great!
|
*/

Route::get('/', function () {
    return view('welcome');
});


Route::get('/users', function () {
    $users = Participants::all(); // Fetch all users from the database

    return view('index', ['users' => $users]); // Pass users data to the view
});

Route::get('/challenges', function () {
    $challenges = Challenge::orderBy('start_date', 'asc')->get();
    return view('challenges', ['challenges' => $challenges]);
});

Route::get('/home', function () {
    return view('home');
});
Route::post('/import', [ImportController::class, 'import'])->name('import');


Route::get('/challenge/{id}/questions', [ChallengeController::class, 'showQuestions']);
