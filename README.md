# BPIC2019

The command-line tool for calculating throughput time and throughput of P2P process presented in BPI Challenge 2019. The system presents a control-flow matching technique for goods receipt, invoice receipt and payment (clear invoice) events within one purchase order. It contains all the calculations that were reported in the BPIC report. The system takes the log in CSV or XES format as input and computes all the corresponding statistics. 

## Content of this distribution

The initial distribution includes:
* out/artifacts/BPIC2019_Task2.jar - Java console application
* src/ - source code of the application

## Usage

The tool requires full address of a log as input parameter and can be executed in the command line via the following command:

```
java -jar BPIC2019_Task2.jar [full_address_of_the_log] 
```

## Requirements

Java 11 or above
