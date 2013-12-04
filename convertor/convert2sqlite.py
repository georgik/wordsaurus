# coding: utf-8
import sqlite3
import struct

char_table = [
  u"0",u"0",u"0",u"0",u"0",u"0",u"0",u"0",u"0",u"0",u"0",u"0",u"0",u"0",u"0",u"0",
  u"0",u"0",u"0",u"0",u"0",u"0",u"0",u"0",u"0",u"0",u"0",u"0",u"0",u"0",u"0",u"0",
  u" ",u"!",u"'",u"#",u"$",u"%",u"&",u"'",u"(",u")",u"*",u"+",u",u",u"-",u".",u"/",
  u"0",u"1",u"2",u"3",u"4",u"5",u"6",u"7",u"8",u"9",u":",u";",u"<",u"=",u">",u"?",
  u"@",u"A",u"B",u"C",u"D",u"E",u"F",u"G",u"H",u"I",u"J",u"K",u"L",u"M",u"N",u"O",
  u"P",u"Q",u"R",u"S",u"T",u"U",u"V",u"W",u"X",u"Y",u"Z",u"[",u"\\",u"]",u"^",u"_",
  u"`",u"a",u"b",u"c",u"d",u"e",u"f",u"g",u"h",u"i",u"j",u"k",u"l",u"m",u"n",u"o",
  u"p",u"q",u"r",u"s",u"t",u"u",u"v",u"w",u"x",u"y",u"z",u"{",u"|",u"}",u"~",u"Z",
  u"€",u"N",u"‚",u"N",u"„",u"…",u"†",u"‡",u"N",u"‰",u"Š",u"‹",u"Ś",u"Ť",u"Ž",u"Ź",
  u"N",u"‘",u"’",u"“",u"”",u"•",u"–",u"—",u"N",u"™",u"š",u"›",u"ś",u"ť",u"ž",u"ź",
  u"N",u"ˇ",u"˘",u"Ł",u"¤",u"Ą",u"¦",u"§",u"¨",u"©",u"Ş",u"«",u"¬",u"S",u"®",u"Ż",
  u"°",u"±",u"˛",u"ł",u"´",u"µ",u"¶",u"·",u"¸",u"ą",u"ş",u"»",u"Ľ",u"˝",u"ľ",u"ż",
  u"Ŕ",u"Á",u"Â",u"Ă",u"Ä",u"Ĺ",u"Ć",u"Ç",u"Č",u"É",u"Ę",u"Ë",u"Ě",u"Í",u"Î",u"Ď",
  u"Đ",u"Ń",u"Ň",u"Ó",u"Ô",u"Ő",u"Ö",u"×",u"Ř",u"Ů",u"Ú",u"Ű",u"Ü",u"Ý",u"Ţ",u"ß",
  u"ŕ",u"á",u"â",u"ă",u"ä",u"ĺ",u"ć",u"ç",u"č",u"é",u"ę",u"ë",u"ě",u"í",u"î",u"ď",
  u"đ",u"ń",u"ň",u"ó",u"ô",u"ő",u"ö",u"÷",u"ř",u"ů",u"ú",u"ű",u"ü",u"ý",u"ţ",u"˙"
   ]

def read_entry(data_file):
    length_byte = data_file.read(1)
    if len(length_byte) == 0:
        return None
    lenght_value = struct.unpack( "B", length_byte)[0]
    result = u""
    for i in range(0,lenght_value):
        data_bytes = data_file.read(1)
        data_ord = struct.unpack( "B", data_bytes)[0]
        # result += str(data_ord) + " - "
        result += char_table[data_ord]
    return result

word_file = open("HESLAVAR.DAT", "rb")
description_file = open("POPISVAR.DAT", "rb")
conn = sqlite3.connect('vks.db')
cur = conn.cursor()
cur.executescript("""
    create table IF NOT EXISTS dictionary(
        record_key,
        record_descr
    );""")

dictionary = {}
key = ""
counter = 0
# while key != None and counter < 100:
while key != None:
    counter += 1
    key = read_entry(word_file)
    value = read_entry(description_file)
    dictionary[key] = value
    # print key, value
    cur.execute("insert into dictionary values (?,?)", (key,value))

cur.close()
conn.commit()
conn.close()

word_file.close()
description_file.close()