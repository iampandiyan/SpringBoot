#!/usr/bin/env python3
"""Interview drill: asks you random questions taken from the "Self-check quiz" of every lesson (day1 and day2).

Usage
  python tools/drill.py                    10 random questions from all lessons, questions only
  python tools/drill.py -n 20              20 questions
  python tools/drill.py --lesson 08 09     only lessons whose folder name starts with 08 or 09
  python tools/drill.py --answers          print the model answer after each question (press Enter to continue)
  python tools/drill.py --seed 1           the same selection again (for a repeatable session)
  python tools/drill.py --list             show how many questions each lesson has

Method: say the answer OUT LOUD in under 90 seconds, then compare with --answers. Repeat the ones you missed.
Reads only the README files; changes nothing.
"""
import argparse, glob, os, random, re, sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")


def parse(path):
    text = open(path, encoding="utf-8").read().replace("\r\n", "\n")
    m = re.search(r"^## 9\. Self-check quiz\n(.*?)(?=^## 10\.)", text, re.S | re.M)
    if not m:
        return []
    body = m.group(1)
    head, _, tail = body.partition("<details>")
    qs = re.findall(r"^(\d+)\. (.+)$", head, re.M)
    # answers: "N. text" lines inside <details>, possibly continued on following lines until the next number
    answers = {}
    for num, txt in re.findall(r"^(\d+)\. (.+?)(?=^\d+\. |\Z|</details>)", tail.split("</details>")[0], re.S | re.M):
        answers[num] = txt.strip()
    return [(n, q.strip(), answers.get(n, "(no answer found)")) for n, q in qs]


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("-n", type=int, default=10)
    ap.add_argument("--lesson", nargs="*", help="folder prefixes such as 08 09")
    ap.add_argument("--answers", action="store_true")
    ap.add_argument("--seed", type=int)
    ap.add_argument("--list", action="store_true")
    a = ap.parse_args()

    items = []
    for path in sorted(glob.glob(os.path.join(ROOT, "day*", "*", "README.md"))):
        folder = os.path.basename(os.path.dirname(path))
        if a.lesson and not any(folder.startswith(p) for p in a.lesson):
            continue
        qs = parse(path)
        if a.list:
            print(f"{folder:35} {len(qs)} questions")
        items += [(folder, n, q, ans) for n, q, ans in qs]
    if a.list:
        print(f"{'total':35} {len(items)}")
        return
    if not items:
        sys.exit("no questions found")
    rnd = random.Random(a.seed)
    for i, (folder, n, q, ans) in enumerate(rnd.sample(items, min(a.n, len(items))), 1):
        print(f"\n[{i}] ({folder}, question {n})\n    {q}")
        if a.answers:
            try:
                input("    (answer aloud, then press Enter) ")
            except EOFError:
                pass
            print("    ANSWER: " + ans.replace("\n", "\n            "))


if __name__ == "__main__":
    main()
