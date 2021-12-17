## requires Blingfire https://github.com/Microsoft/BlingFire 

import argparse
import os
from pathlib import Path

from blingfire import text_to_sentences

if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description="Sentence segmentation")

    parser.add_argument("--input", help="Input file", required=True)
    parser.add_argument("--output", help="Output file", required=True)

    args = parser.parse_args()

    input = args.input
    output = args.output

    if os.path.isfile(input):
        input_path = Path(input)
        output_path = Path(output)

        with open(output_path, 'w') as fo:
            with open(input_path, 'r') as fi:
                for line in fi:
                    for sentence in text_to_sentences(line).split("\n"):
                        fo.write(sentence + "\n")


    else:
        parser.print_help()
