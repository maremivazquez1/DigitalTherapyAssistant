# Digital Therapy Assistant - Setup and Installation Guide

This guide provides step-by-step instructions to set up your development environment and run the Digital Therapy Assistant application.

## Prerequisites

* **Operating System:** macOS (Homebrew instructions are specific to macOS)

## Step 1: Install Essential Tools

1.  **Java JDK:**
    * Download and install the latest JDK from [Oracle](https://www.oracle.com/java/technologies/javase-downloads.html) or [OpenJDK](https://openjdk.java.net/).
    * Verify installation: `java -version`

2.  **IntelliJ IDEA Community Edition:**
    * Download and install from [JetBrains](https://www.jetbrains.com/idea/download/).

3.  **Homebrew (if not already installed):**
    * `/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"`
    * Verify installation: `brew --version`

4.  **Maven:**
    * `brew install maven`
    * Verify installation: `mvn -version`

5.  **MySQL:**
    * `brew install mysql`
    * `brew services start mysql`


## Step 2: Configure MySQL

1.  **Login to MySQL:**
    * `mysql -u root -p` (enter your MySQL root password when prompted)

2.  **Create Database:**
    * `CREATE DATABASE cbt;`

3.  **Use Database:**
    * `USE cbt;`

4.  **Create Users Table:**
    ```sql
    CREATE TABLE users (
        id INT AUTO_INCREMENT PRIMARY KEY,
        username VARCHAR(255) NOT NULL UNIQUE,
        password VARCHAR(255) NOT NULL,
        email VARCHAR(255) NOT NULL UNIQUE,
        first_name VARCHAR(255),
        last_name VARCHAR(255),
        phone_number VARCHAR(20),
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    );
    ```

## Step 3: Set Up the Application

1.  **Clone Repository:**
    * `git clone https://github.com/maremivazquez1/DigitalTherapyAssistant.git`

2.  **Import into IntelliJ IDEA:**
    * Open IntelliJ IDEA and import the cloned project.


## Step 4: Run and Test

1.  **Build Project:**
    * `mvn clean install`

2.  **Run Application:**
    * `mvn spring-boot:run`

3.  **Test Endpoints:**
    * **Root:** `http://localhost:8080/` (should display "Spring Boot is running!")
    * **Registration:** `http://localhost:8080/api/register` (should display "Registration implementation coming soon!")

## Summary of Commands

```bash
# Install tools
/bin/bash -c "$(curl -fsSL [https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh](https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh))"
brew install maven
brew install mysql
brew services start mysql

# MySQL setup
mysql -u root -p
CREATE DATABASE cbt;
USE cbt;
# (Create users table - see SQL code above)

# Application setup
git clone [https://github.com/maremivazquez1/DigitalTherapyAssistant.git](https://github.com/maremivazquez1/DigitalTherapyAssistant.git)
# (Configure application.properties - see instructions above)

# Run and test
mvn clean install
mvn spring-boot:run