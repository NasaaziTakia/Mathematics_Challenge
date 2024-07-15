<!-- questions.blade.php -->
@extends('master')

@section('content')
    <div class="container mt-5">
        <h2>Questions for Challenge {{ $challenge->challengeNumber }}</h2>
        
        @if (empty($questionsWithAnswers))
            <div class="alert alert-info">
                No questions found for this challenge.
            </div>
        @else
            <div class="table-responsive">
                <table class="table table-bordered">
                    <thead class="thead-dark">
                        <tr>
                            <th>Question</th>
                            <th>Answer</th>
                        </tr>
                    </thead>
                    <tbody>
                        @foreach ($questionsWithAnswers as $qa)
                            <tr>
                                <td>{{ $qa['question'] }}</td>
                                <td>{{ $qa['answer'] }}</td>
                            </tr>
                        @endforeach
                    </tbody>
                </table>
            </div>
        @endif
    </div>
@endsection
