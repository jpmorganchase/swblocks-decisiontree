[![License](http://img.shields.io/badge/license-Apache_2.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![Build Status](https://travis-ci.org/jpmorganchase/swblocks-decisiontree.svg?branch=master)](https://travis-ci.org/jpmorganchase/swblocks-decisiontree)

# swblocks-decisiontree

## Overview

swblocks-decisiontree library is a high performance, highly flexible service which evaluates inputs to a set of rules to identify one and only one output rule which in term results in a set of outputs. It can be used to model complex conditional processing within an application.
<br>Example:

| Rule    | Input1: Day of week | Input2: Outstanding Work | Input3: Boss's mood | Output 1: Leave Time | Output2: After Work |
| ------- | ------------------- | ------------------------ | ------------------- | -------------------- | ------------------- |
| 1       | Monday              |  *                       | *                   | 5 pm                 | Pick up children    | 
| 2       | *                   | Low | * | 6:45 pm | Go to gym |
| 3       | Thursday            | * | Good | 6:30 pm | Go to pub |
| 4 | Friday | * | * | 5 pm | Party time |
| 5 | * | * | * | 7 pm | Go home |
| 6 | * | * | Angry | 10 pm | More work |

The rules are evaluated based on the precedence of matched data from left to right.  A matching value gives a 1 and a wildcard match a 0, the highest value is picked.
In this example the employees life after work on certain days takes precedence over the boss.
If the following input values are sent in:
<br><code>Inputs: Day of Week: Monday, Outstanding Work: High, Boss's mood: Angry
<br>This matches rules 1, 5 and 6. Rule 1 gives 100, Rule 5 gives 000 and Rule 6 gives 001, rule 1 is picked. 
<br>Output: Leave Time: 5pm, After Work: Pick up children
</code><p>
If the order of the columns are changed to make the Boss's mood higher importance, then this gives.

| Rule    | Input1: Boss's Mode | Input2: Outstanding Work | Input3: Day of week | Output 1: Leave Time | Output2: After Work |
| ------- | ------------------- | ------------------------ | ------------------- | -------------------- | ------------------- |
| 1 | * | * | Monday | 5 pm | Pick up children    | 
| 2 | *  | Low | * | 6:45 pm | Go to gym |
| 3 | Good | * | Thursday | 6:30 pm | Go to pub |
| 4 | * | * | Friday | 5 pm | Party time |
| 5 | * | * | * | 7 pm | Go home |
| 6 | Angry | * | * | 10 pm | More work |

<br>If the same inputs are sent in:
<br><code>Inputs: Day of Week: Monday, Outstanding Work: High, Boss's mood: Angry
<br>This matches rules 1, 5 and 6. Rule 1 gives 001, Rule 5 gives 000 and Rule 6 gives 100, rule 6 is picked. 
<br>Output: Leave Time: 10pm, After Work: More work
</code><p>
<p>

### Structure of code
The code is in a multi-module structure project structure.
#### Core
The Core project contains the main algorithms for the tree building and construction.
#### Change
The Change project provides an API to modify a DecisionTree in an audited and controlled manor.
#### Persistence-json-jackson
A persistence module for writing and reading using a JSON structure and the Jackson JSON code.
#### Persistence-cassandra
A persistence module for writing and reading using Cassandra as a storage.
##### Examples
A set of example code to demonstrate usage of the DecisionTree.
<p>

## License

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this library except in compliance with the License. You may obtain a copy of the License at:

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

See the [LICENSE](LICENSE) file for additional license information

 
## Java Build

The project is built with [Gradle](http://gradle.org/) using this [build.gradle](build.gradle) file.

You require the following to build swblocks-jbl:

* Latest stable [Oracle JDK 8](http://www.oracle.com/technetwork/java/)

Default target provides a full clean, build, and install into local maven repository
