<!-- resources/views/home.blade.php -->
@extends('master')

@section('content')
    <div class="d-flex justify-content-center align-items-center">
        <form method="POST" action="{{ route('import') }}" enctype="multipart/form-data" class="p-4 border rounded"
            style="width: 100%; max-width: 80%;">
            @csrf

            {{-- Display validation errors --}}
            @if (session('error'))
                <div class="alert alert-danger">
                    {{ session('error') }}
                </div>
            @endif

            @if (session('success'))
                <div class="alert alert-success">{{ session('success') }}</div>
            @endif
            <div class="row mb-3">
                <div class="col">
                    <label for="challengeNumber" class="form-label">Challenge Number</label>
                    <input type="text" class="form-control" id="challengeNumber" name="challengeNumber"
                        placeholder="Enter challenge" value="{{ old('challengeNumber') }}">
                </div>
                <div class="col">
                    <label for="questionsPerAttempt" class="form-label">Number of Questions per attempt</label>
                    <input type="text" class="form-control" id="questionsPerAttempt" name="questionsPerAttempt"
                        placeholder="Enter number of questions per attempt" value="{{ old('questionsPerAttempt') }}">
                </div>
            </div>
            <div class="row mb-3">
                <div class="col">
                    <label for="startDate" class="form-label">Start Date</label>
                    <input type="date" class="form-control" id="startDate" name="startDate"
                        value="{{ old('startDate') }}">
                </div>
                <div class="col">
                    <label for="endDate" class="form-label">End Date</label>
                    <input type="date" class="form-control" id="endDate" name="endDate" value="{{ old('endDate') }}">
                </div>
            </div>
            <div class="row mb-3">
                <div class="col">
                    <label for="file" class="form-label">Choose Questions File (.xlsx, .xls)</label>
                    <input type="file" class="form-control" id="file" name="questionsFile" accept=".xlsx, .xls"
                        required onchange="handleFileSelect(event)">
                    <select class="form-control mt-3" id="questionsDropdown" name="questionsDropdown"
                        style="display: none;">
                    </select>
                </div>
                <div class="col">
                    <label for="answersFile" class="form-label">Choose Answers File (.xlsx, .xls)</label>
                    <input type="file" class="form-control" id="answersFile" name="answersFile" accept=".xlsx, .xls"
                        required onchange="handleAnswersFileSelect(event)">
                    <select class="form-control mt-3" id="answersDropdown" name="answersDropdown" style="display: none;">
                    </select>
                </div>
            </div>
            <div class="row mb-3">
                <div class="col">
                    <label for="duration" class="form-label">Duration</label>
                    <input type="number" class="form-control" id="duration" name="duration"
                        placeholder="Enter duration in minutes" value="{{ old('duration') }}">
                </div>
            </div>
            <button type="submit" class="btn btn-primary">Submit</button>

        </form>
    </div>
    <script>
        function validateForm() {
            // Retrieve form inputs
            var challengeNumber = document.getElementById('challengeNumber').value.trim();
            var questionsPerAttempt = document.getElementById('questionsPerAttempt').value.trim();
            var startDate = document.getElementById('startDate').value.trim();
            var endDate = document.getElementById('endDate').value.trim();
            var file = document.getElementById('file').value.trim();
            var answersFile = document.getElementById('answersFile').value.trim();
            var duration = document.getElementById('duration').value.trim();

            // Check if any field is empty
            if (challengeNumber === '' || questionsPerAttempt === '' || startDate === '' || endDate === '' || file === '' ||
                answersFile === '' || duration === '') {
                alert('Please fill in all fields.');
                return false; // Prevent form submission
            }

            return true; // Allow form submission if validation passes
        }

        // Set minimum start date to today and update end date
        document.getElementById('startDate').setAttribute('min', new Date().toISOString().split('T')[0]);
        document.getElementById('startDate').addEventListener('change', function() {
            document.getElementById('endDate').setAttribute('min', this.value);
        });

        // Handle file selection for questions and display its contents
        function handleFileSelect(event) {
            const file = event.target.files[0];
            const reader = new FileReader();

            reader.onload = function(e) {
                const data = new Uint8Array(e.target.result);
                const workbook = XLSX.read(data, {
                    type: 'array'
                });
                const firstSheetName = workbook.SheetNames[0];
                const worksheet = workbook.Sheets[firstSheetName];
                const jsonData = XLSX.utils.sheet_to_json(worksheet, {
                    header: 1
                });

                populateDropdown('questionsDropdown', jsonData);
            };

            reader.readAsArrayBuffer(file);
        }

        // Handle file selection for answers and display its contents
        function handleAnswersFileSelect(event) {
            const file = event.target.files[0];
            const reader = new FileReader();

            reader.onload = function(e) {
                const data = new Uint8Array(e.target.result);
                const workbook = XLSX.read(data, {
                    type: 'array'
                });
                const firstSheetName = workbook.SheetNames[0];
                const worksheet = workbook.Sheets[firstSheetName];
                const jsonData = XLSX.utils.sheet_to_json(worksheet, {
                    header: 1
                });

                populateDropdown('answersDropdown', jsonData);
            };

            reader.readAsArrayBuffer(file);
        }

        // Populate dropdown based on data
        function populateDropdown(id, data) {
            const dropdown = document.getElementById(id);
            dropdown.innerHTML = ''; // Clear previous options

            const defaultOption = document.createElement('option');
            defaultOption.text = id === 'questionsDropdown' ? 'Questions from excel' : 'Answers from excel';
            defaultOption.disabled = true;
            defaultOption.selected = true;
            dropdown.appendChild(defaultOption);

            data.forEach((row, index) => {
                if (index === 0) return; // Skip header row

                const option = document.createElement('option');
                option.value = index;
                id === 'questionsDropdown' ? option.text = `${row[0]} - ${row[1]}(${row[2]})` : option.text =
                    `${row[0]} - ${row[1]}`;

                dropdown.appendChild(option);
            });

            dropdown.style.display = 'block'; // Show the dropdown
        }
    </script>
@endsection
