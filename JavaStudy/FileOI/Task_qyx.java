package JavaStudy.FileOI;
import java.io.*;
import java.util.*;

class Task {

    final int MAX_SIZE = 2048; 

    public void input() {
        Scanner scanner = new Scanner(System.in);
        int n = Integer.parseInt(scanner.nextLine());
        List<String> complaints = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            String[] parts = scanner.nextLine().split(" ");
            String formattedComplaint = String.format("%s_%s_%s_%s", 
                parts[0], parts[1], 
                String.join("_", Arrays.copyOfRange(parts, 2, 5)), 
                String.join("_", Arrays.copyOfRange(parts, 5, parts.length)));
            complaints.add(formattedComplaint);
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("Input.txt"))) {
            for (String complaint : complaints) {
                writer.write(complaint);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void changeGrade() {
        Map<String, String> complaintMap = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader("Input.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("_");
                complaintMap.put(parts[0], parts[3]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<String> updatedGrades = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader("grade.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("_");
                if (complaintMap.containsKey(parts[0])) {
                    int score = Integer.parseInt(parts[2]);
                    updatedGrades.add(String.format("%s_%s_%d*", parts[0], parts[1], score + 3));
                } else {
                    updatedGrades.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("grade.txt"))) {
            for (String grade : updatedGrades) {
                writer.write(grade);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void encrypt() {
        StringBuilder encryptedContent = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader("grade.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                for (char c : line.toCharArray()) {
                    encryptedContent.append((char) (c + 3));
                }
                encryptedContent.append('\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("encrypted.txt"))) {
            writer.write(encryptedContent.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Task task = new Task();
        // Uncomment the following lines to test each method
        // task.input();
        // task.changeGrade();
        // task.encrypt();
    }
}



