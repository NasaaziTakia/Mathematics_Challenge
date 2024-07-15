<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class Challenge extends Model
{
    protected $table = 'challenge'; // Specify your table name if different from 'challenges'
    public $timestamps = false; 
    protected $fillable = [
        'challengeNumber', 'start_date', 'end_date', 'duration', 'num_questions'
    ];

}
