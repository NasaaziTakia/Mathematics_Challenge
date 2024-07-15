package Participant;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) {
        Socket soc = null;
        PrintWriter out = null;
        BufferedReader in = null;
        Scanner scanner = null;
        FileInputStream fis = null;

        try {
            System.out.println("Client started");
            soc = new Socket("localhost", 9806);

            out = new PrintWriter(soc.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(soc.getInputStream()));
            scanner = new Scanner(System.in);

            System.out.println("Are you already registered? (yes/no): ");
            String response = scanner.nextLine().trim().toLowerCase();

            if (response.equals("yes")) {
                // Handle login process
                if (handleLogin(scanner, out, in)) {
                    // Successfully logged in
                    System.out.println("Enter 'view challenge' to see available challenges or 'logout' to exit.");
                    while (true) {
                        String command = scanner.nextLine().trim().toLowerCase();
                        if (command.equals("view challenge")) {
                            challengeFunctions.ViewChallenges(scanner);
                        } else if (command.equals("logout")) {
                            System.out.println("Logging out...");
                            break;
                        } else {
                            System.out.println("Invalid command. Enter 'view challenge' to see available challenges or 'logout' to exit.");
                        }
                    }
                }
            } else if (response.equals("no")) {
                // Handle registration process
                handleRegistration(scanner, out, in, soc);
            } else {
                System.out.println("Invalid input. Please start over.");
                closeResources(out, in, scanner, soc, fis);
                return;
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeResources(out, in, scanner, soc, fis);
        }
    }

    private static boolean handleLogin(Scanner scanner, PrintWriter out, BufferedReader in) throws IOException {
        System.out.println("Enter login command: ");
        System.out.println("username student_number");

        String loginCommand = scanner.nextLine().trim();
        out.println("login " + loginCommand); // Send login command to the server

        // Receive server response for login
        String serverResponse = in.readLine();
        System.out.println("Server response for login: " + serverResponse);
        return serverResponse.equalsIgnoreCase("login successful");
    }

    private static void handleRegistration(Scanner scanner, PrintWriter out, BufferedReader in, Socket soc) throws IOException {
        FileInputStream fis = null;
        try {
            System.out.println("Enter registration command: ");
            System.out.println("username firstname lastname emailAddress date_of_birth student_number image_file.png");

            String registerCommand = scanner.nextLine().trim();
            out.println("register " + registerCommand); // Send register command to the server

            String[] details = registerCommand.split(" ");
            if (details.length < 7) {
                System.out.println("Invalid input data. Please provide all required information.");
                return;
            }

            // Check if image file exists
            String imageFilePath = details[details.length - 1];
            File imageFile = new File(imageFilePath);
            if (!imageFile.exists()) {
                System.out.println("Image file not found: " + imageFilePath);
                return;
            }

            // Send image file to server
            System.out.println("Sending image file...");
            fis = new FileInputStream(imageFile);
            OutputStream os = soc.getOutputStream();

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();

            // Shutdown output to notify server that file transfer is complete
            soc.shutdownOutput();

            // Receive server response for registration
            String serverResponse = in.readLine();
            System.out.println("Server response for registration: " + serverResponse);

        } finally {
            if (fis != null) fis.close();
        }
    }

    private static void closeResources(PrintWriter out, BufferedReader in, Scanner scanner, Socket soc, FileInputStream fis) {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (scanner != null) scanner.close();
            if (fis != null) fis.close();
            if (soc != null) soc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

