

| set      | entities  | uniq_entities  | classes  | materials  |
|----------|-----------|----------------|----------|------------|
| training | 13648     | 4512           | 8        | 9268       |
| holdout  | 5728      | 2817           | 8        | 3292       |
| ratio    | 41.97%    | 62.43%         | 100.00%  | 35.52%     |



| set      | fabrication  | formula  | shape   | substrate  | name   | variable  | value  | doping  |
|----------|--------------|----------|---------|------------|--------|-----------|--------|---------|
| training | 94           | 6301     | 809     | 32         | 1930   | 1795      | 1895   | 792     |
| holdout  | 44           | 2569     | 841     | 148        | 949    | 449       | 463    | 265     |
| ratio    | 46.81%       | 40.77%   | 103.96% | 462.50%    | 49.17% | 25.01%    | 24.43% | 33.46%  |



Out of domain

| label       | # in domain   | # in domain uniques   | # out domain  | # out domain unique   |
|-------------|---------------|-----------------------|---------------|-----------------------|
| fabrication | 3             | 2                     | 41            | 28                    |
| formula     | 609           | 194                   | 1960          | 1253                  |
| shape       | 776           | 26                    | 65            | 42                    |
| substrate   | 12            | 3                     | 136           | 54                    |
| name        | 467           | 39                    | 482           | 212                   |
| variable    | 425           | 14                    | 24            | 13                    |
| value       | 162           | 67                    | 301           | 252                   |
| doping      | 111           | 24                    | 154           | 74                    |