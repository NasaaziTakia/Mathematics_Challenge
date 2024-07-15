<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class Answers extends Model
{
    protected $table = 'answers'; // Specify your table name if different from 'challenges'
    public $timestamps = false;
    protected $fillable = [
        'question_id', 'answer_text'
    ];
}
