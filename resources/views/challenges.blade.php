<!-- index.blade.php -->
@extends('master')

@section('content')
    <div class="container mt-5">
        <h2 class="mb-4">Challenges</h2>
        
        @if($challenges->isEmpty())
            <div class="alert alert-info">
                No challenges available.
            </div>
        @else
            <div class="table-responsive">
                <table class="table table-hover table-bordered">
                    <thead class="thead-dark">
                        <tr>
                            <th>Challenge Number</th>
                            <th>Start Date</th>
                            <th>End Date</th>
                            <th>Duration (minutes)</th>
                            <th>Number of Questions</th>
                        </tr>
                    </thead>
                    <tbody>
                        @foreach ($challenges as $challenge)
                            <tr data-challenge-id="{{ $challenge->id }}" class="challenge-row">
                                <td>{{ $challenge->challengeNumber }}</td>
                                <td>{{ $challenge->start_date }}</td>
                                <td>{{ $challenge->end_date }}</td>
                                <td>{{ $challenge->duration }}</td>
                                <td>{{ $challenge->num_questions }}</td>
                            </tr>
                        @endforeach
                    </tbody>
                </table>
            </div>
        @endif
    </div>

    <script>
        // Add click event listener to table rows with class 'challenge-row'
        document.addEventListener('DOMContentLoaded', function () {
            const rows = document.querySelectorAll('.challenge-row');
            rows.forEach(row => {
                row.addEventListener('click', function () {
                    const challengeId = this.getAttribute('data-challenge-id');
                    window.location.href = `/challenge/${challengeId}/questions`;
                });
            });
        });
    </script>
@endsection
