# AlgorithmViualizer

An interactive Java Swing application for visualizing and comparing classic sorting algorithms through real-time animated execution.

---

## 📌 Overview

**AlgorithmViualizer** is a desktop-based algorithm visualization tool built using **pure Java Swing**.  
It provides an intuitive, real-time view of how different sorting algorithms operate internally by animating comparisons, swaps, and final sorted states.

The project focuses on **clarity, correctness, and responsiveness**, making it suitable for:
- Learning algorithm behavior
- Teaching data structures
- Demonstrating multithreading and UI rendering in Java
- Showcasing clean Swing-based application design

---

## 🚀 Features

- Visualizes sorting using **vertical bar (histogram) representation**
- Supports multiple sorting algorithms:
  - Bubble Sort
  - Quick Sort
  - Merge Sort
- Step-by-step animated execution
- Adjustable animation speed
- Fully responsive UI (no freezing during sorting)
- Color-coded visualization for clarity

---

## 🎨 Color Coding

| Color  | Meaning |
|------|--------|
| White | Default / Unprocessed elements |
| Red   | Elements currently being compared or swapped |
| Green | Elements confirmed as sorted |

---

## 🧠 Algorithms Implemented

### Bubble Sort
- Repeatedly compares adjacent elements
- Visually highlights swaps
- Confirms sorted elements incrementally

### Quick Sort
- Uses partitioning around a pivot
- Recursively sorts subarrays
- Demonstrates divide-and-conquer behavior

### Merge Sort
- Recursively splits the array
- Merges sorted halves step-by-step
- Clearly shows reconstruction of the array

---

## 🛠️ Controls

- **Algorithm Selector** – Choose the sorting algorithm to visualize
- **Randomize** – Generates a new random dataset
- **Start** – Begins the visualization (disabled during execution)
- **Speed Slider** – Controls animation delay in milliseconds

---

## ⚙️ Technical Details

- Language: **Java**
- UI Framework: **Java Swing**
- Architecture:
  - Single runnable class
  - Background execution using `SwingWorker`
  - Custom painting with `JPanel.paintComponent`
- No external libraries
- Thread-safe UI updates
- Fully contained in a single `.java` file

---

## ▶️ How to Run

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/AlgorithmViualizer.git
