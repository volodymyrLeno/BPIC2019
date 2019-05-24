# BPIC2019

The command-line tool for calculating throughput time and throughput of P2P process presented in BPI Challenge 2019. The system presents a control-flow matching technique for goods receipt, invoice receipt and payment (clear invoice) events within one purchase order. It contains all the calculations that were reported in the BPIC report. The system takes the log in CSV or XES format as input and computes all the corresponding statistics. 

## Content of this distribution

The initial distribution includes:
* BPIC2019_Task2.jar - Java console application
* logs/ - the logs that were used (original BPIC 2019 log partitioned accordingly the data flows)

## Usage

The tool requires full address of a log as input parameter and can be executed in the command line via the following command:

```
java -jar BPIC2019_Task2.jar [full address of the log] 
```

## Requirements

Java 11 or above
