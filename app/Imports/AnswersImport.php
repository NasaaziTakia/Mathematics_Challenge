<?php

namespace App\Imports;

use App\Models\Question;
use Maatwebsite\Excel\Concerns\ToModel;
use Maatwebsite\Excel\Concerns\WithHeadingRow;
use Illuminate\Support\Collection;
use Maatwebsite\Excel\Concerns\ToCollection;

class AnswersImport implements ToCollection, WithHeadingRow
{
    // /**
    //  * @param array $row
    //  *
    //  * @return \Illuminate\Database\Eloquent\Model|null
    //  */
    // public function model(array $row)
    // {
    //     return new Question([
    //         'number'   => $row['number'],
    //         'question' => $row['question'],
    //         'marks'    => $row['marks'],
    //     ]);
    // }
    public $data;

    public function collection(Collection $rows)
    {
        $this->data = $rows;
    }
}
