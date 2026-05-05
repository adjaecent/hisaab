# hisaab
_hisāba (हिसाब) means "to account for" in Hindi._

A [Babashka](https://babashka.org) script to parse and generate reports from HDFC bank and credit card statements.

## Setup

Generate a config file (tweak it to add your own tags and filters):

```
# symlinks resources/hisaab.conf.toml to $HOME/hisaab.conf.toml
$ make confgen
```

## CLI usage

HDFC bank statement (requires the delimited `.txt` format):

```
$ make hdfc-bank FILE=<path-to-file>.txt

==> Summary
|                  :period | :withdrawls | :deposits | :expenditure | :closing-balance |
|--------------------------+-------------+-----------+--------------+------------------|
| 2024-01-01 to 2024-02-01 |     80000.0 |   40000.0 |      40000.0 |           120000 |

==> Breakdown
|       :category | :debit | :credit |    :net |
|-----------------+--------+---------+---------|
|     credit-card |  30000 |       0 |  -30000 |
|       groceries |   4000 |       0 |   -4000 |
|       household |  10000 |       0 |  -10000 |
|     investments |  20000 |   15000 |   -5000 |
|        untagged |  16000 |   25000 |    9000 |
|           ...   |    ... |     ... |     ... |
```

HDFC credit card statement (requires the delimited `.csv` format):

```
$ make hdfc-cc FILE=<path-to-file>.csv

==> Summary
|                  :period | :total-credits | :total-debits |
|--------------------------+----------------+---------------|
| 2024-01-16 to 2024-02-15 |       50000.00 |      65000.00 |

==> Expenditure Breakdown
|     :category |   :amount |
|---------------+-----------|
|           fnb |   8000.00 |
|           tax |   5000.00 |
|     transport |   3000.00 |
| subscriptions |   1200.00 |
|        ...    |       ... |
```
