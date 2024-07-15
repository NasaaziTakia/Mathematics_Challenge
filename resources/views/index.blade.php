<!-- resources/views/users/index.blade.php -->

<!DOCTYPE html>
<html>
<head>
    <title>Users List</title>
</head>
<body>
    <h1>Users List</h1>

    <ul>
        @foreach ($users as $user)
            <li>{{ $user->username }} - {{ $user->firstname }}</li>
        @endforeach
    </ul>
</body>
</html>
