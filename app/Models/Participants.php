<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Participants extends Model
{
    // Table name if different from 'users'
    protected $table = 'participants';
    public $timestamps = false;
    // Fillable attributes
    protected $fillable = [
        'username', 'firstname',
    ];

    // Hidden attributes
    // protected $hidden = [
    //     'password',
    // ];
}
