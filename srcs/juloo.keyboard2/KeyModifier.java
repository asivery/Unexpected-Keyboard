package juloo.keyboard2;

import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import java.util.HashMap;

class KeyModifier
{
  /** Cache key is KeyValue's name */
  private static HashMap<String, HashMap<Pointers.Modifiers, KeyValue>> _cache =
    new HashMap<String, HashMap<Pointers.Modifiers, KeyValue>>();

  /** Represents a removed key, because a cache entry can't be [null]. */
  private static final KeyValue removed_key = KeyValue.getKeyByName("removed");

  /** Modify a key according to modifiers. */
  public static KeyValue modify(KeyValue k, Pointers.Modifiers mods)
  {
    if (k == null)
      return null;
    int n_mods = mods.size();
    HashMap<Pointers.Modifiers, KeyValue> ks = cacheEntry(k);
    KeyValue r = ks.get(mods);
    if (r == null)
    {
      r = k;
      /* Order: Fn, Shift, accents */
      for (int i = 0; i < n_mods; i++)
        r = modify(r, mods.get(i));
      r = remove_placeholders(r);
      ks.put(mods, r);
    }
    return (r == removed_key) ? null : r;
  }

  public static KeyValue modify(KeyValue k, KeyValue.Modifier mod)
  {
    switch (mod)
    {
      case CTRL:
      case ALT:
      case META: return turn_into_keyevent(k);
      case FN: return apply_fn(k);
      case SHIFT: return apply_shift(k);
      case GRAVE: return apply_dead_char(k, '\u02CB');
      case AIGU: return apply_dead_char(k, '\u00B4');
      case CIRCONFLEXE: return apply_dead_char(k, '\u02C6');
      case TILDE: return apply_dead_char(k, '\u02DC');
      case CEDILLE: return apply_dead_char(k, '\u00B8');
      case TREMA: return apply_dead_char(k, '\u00A8');
      case CARON: return apply_dead_char(k, '\u02C7');
      case RING: return apply_dead_char(k, '\u02DA');
      case MACRON: return apply_dead_char(k, '\u00AF');
      case OGONEK: return apply_dead_char(k, '\u02DB');
      case DOT_ABOVE: return apply_dead_char(k, '\u02D9');
      case DOUBLE_AIGU: return apply_map_char(k, map_char_double_aigu);
      case ORDINAL: return apply_map_char(k, map_char_ordinal);
      case SUPERSCRIPT: return apply_map_char(k, map_char_superscript);
      case SUBSCRIPT: return apply_map_char(k, map_char_subscript);
      case ARROWS: return apply_map_char(k, map_char_arrows);
      case BOX: return apply_map_char(k, map_char_box);
      case SLASH: return apply_map_char(k, map_char_slash);
      case ARROW_RIGHT: return apply_combining(k, "\u20D7");
      default: return k;
    }
  }

  private static KeyValue apply_map_char(KeyValue k, Map_char map)
  {
    switch (k.getKind())
    {
      case Char:
        char kc = k.getChar();
        char c = map.apply(kc);
        return (kc == c) ? k : k.withChar(c);
      default: return k;
    }
  }

  private static KeyValue apply_dead_char(KeyValue k, char dead_char)
  {
    switch (k.getKind())
    {
      case Char:
        char kc = k.getChar();
        char c = (char)KeyCharacterMap.getDeadChar(dead_char, kc);
        return (c == 0 || kc == c) ? k : k.withChar(c);
      default: return k;
    }
  }

  private static KeyValue apply_combining(KeyValue k, String combining)
  {
    switch (k.getKind())
    {
      case Char:
        return k.withString(String.valueOf(k.getChar()) + combining);
      default: return k;
    }
  }

  private static KeyValue apply_shift(KeyValue k)
  {
    switch (k.getKind())
    {
      case Char:
        char kc = k.getChar();
        char c = map_char_shift(kc);
        if (kc == c)
          c = Character.toUpperCase(kc);
        return (kc == c) ? k : k.withChar(c);
      case String:
        return k.withString(k.getString().toUpperCase());
      default: return k;
    }
  }

  private static KeyValue apply_fn(KeyValue k)
  {
    String name;
    switch (k.name)
    {
      case "1": name = "f1"; break;
      case "2": name = "f2"; break;
      case "3": name = "f3"; break;
      case "4": name = "f4"; break;
      case "5": name = "f5"; break;
      case "6": name = "f6"; break;
      case "7": name = "f7"; break;
      case "8": name = "f8"; break;
      case "9": name = "f9"; break;
      case "0": name = "f10"; break;
      case "f11_placeholder": name = "f11"; break;
      case "f12_placeholder": name = "f12"; break;
      case "up": name = "page_up"; break;
      case "down": name = "page_down"; break;
      case "left": name = "home"; break;
      case "right": name = "end"; break;
      case "<": name = "«"; break;
      case ">": name = "»"; break;
      case "{": name = "‹"; break;
      case "}": name = "›"; break;
      case "[": name = "‘"; break;
      case "]": name = "’"; break;
      case "(": name = "“"; break;
      case ")": name = "”"; break;
      case "'": name = "‚"; break;
      case "\"": name = "„"; break;
      case "-": name = "–"; break;
      case "_": name = "—"; break;
      case "^": name = "¬"; break;
      case "%": name = "‰"; break;
      case "=": name = "≈"; break;
      case "u": name = "µ"; break;
      case "a": name = "æ"; break;
      case "o": name = "œ"; break;
      case "esc": name = "insert"; break;
      case "*": name = "°"; break;
      case ".": name = "…"; break;
      case ",": name = "·"; break;
      case "!": name = "¡"; break;
      case "?": name = "¿"; break;
      case "tab": name = "\\t"; break;
      case "space": name = "nbsp"; break;
      case "↖": name = "⇖"; break;
      case "↑": name = "⇑"; break;
      case "↗": name = "⇗"; break;
      case "←": name = "⇐"; break;
      case "→": name = "⇒"; break;
      case "↙": name = "⇙"; break;
      case "↓": name = "⇓"; break;
      case "↘": name = "⇘"; break;
      // Currency symbols
      case "e": name = "€"; break;
      case "l": name = "£"; break;
      case "r": name = "₹"; break;
      case "y": name = "¥"; break;
      case "c": name = "¢"; break;
      case "p": name = "₱"; break;
      case "€": case "£": return removed_key; // Avoid showing these twice
      default: return k;
    }
    return KeyValue.getKeyByName(name);
  }

  private static KeyValue turn_into_keyevent(KeyValue k)
  {
    if (k.getKind() != KeyValue.Kind.Char)
      return k;
    int e;
    switch (k.getChar())
    {
      case 'a': e = KeyEvent.KEYCODE_A; break;
      case 'b': e = KeyEvent.KEYCODE_B; break;
      case 'c': e = KeyEvent.KEYCODE_C; break;
      case 'd': e = KeyEvent.KEYCODE_D; break;
      case 'e': e = KeyEvent.KEYCODE_E; break;
      case 'f': e = KeyEvent.KEYCODE_F; break;
      case 'g': e = KeyEvent.KEYCODE_G; break;
      case 'h': e = KeyEvent.KEYCODE_H; break;
      case 'i': e = KeyEvent.KEYCODE_I; break;
      case 'j': e = KeyEvent.KEYCODE_J; break;
      case 'k': e = KeyEvent.KEYCODE_K; break;
      case 'l': e = KeyEvent.KEYCODE_L; break;
      case 'm': e = KeyEvent.KEYCODE_M; break;
      case 'n': e = KeyEvent.KEYCODE_N; break;
      case 'o': e = KeyEvent.KEYCODE_O; break;
      case 'p': e = KeyEvent.KEYCODE_P; break;
      case 'q': e = KeyEvent.KEYCODE_Q; break;
      case 'r': e = KeyEvent.KEYCODE_R; break;
      case 's': e = KeyEvent.KEYCODE_S; break;
      case 't': e = KeyEvent.KEYCODE_T; break;
      case 'u': e = KeyEvent.KEYCODE_U; break;
      case 'v': e = KeyEvent.KEYCODE_V; break;
      case 'w': e = KeyEvent.KEYCODE_W; break;
      case 'x': e = KeyEvent.KEYCODE_X; break;
      case 'y': e = KeyEvent.KEYCODE_Y; break;
      case 'z': e = KeyEvent.KEYCODE_Z; break;
      case '0': e = KeyEvent.KEYCODE_0; break;
      case '1': e = KeyEvent.KEYCODE_1; break;
      case '2': e = KeyEvent.KEYCODE_2; break;
      case '3': e = KeyEvent.KEYCODE_3; break;
      case '4': e = KeyEvent.KEYCODE_4; break;
      case '5': e = KeyEvent.KEYCODE_5; break;
      case '6': e = KeyEvent.KEYCODE_6; break;
      case '7': e = KeyEvent.KEYCODE_7; break;
      case '8': e = KeyEvent.KEYCODE_8; break;
      case '9': e = KeyEvent.KEYCODE_9; break;
      case '`': e = KeyEvent.KEYCODE_GRAVE; break;
      case '-': e = KeyEvent.KEYCODE_MINUS; break;
      case '=': e = KeyEvent.KEYCODE_EQUALS; break;
      case '[': e = KeyEvent.KEYCODE_LEFT_BRACKET; break;
      case ']': e = KeyEvent.KEYCODE_RIGHT_BRACKET; break;
      case '\\': e = KeyEvent.KEYCODE_BACKSLASH; break;
      case ';': e = KeyEvent.KEYCODE_SEMICOLON; break;
      case '\'': e = KeyEvent.KEYCODE_APOSTROPHE; break;
      case '/': e = KeyEvent.KEYCODE_SLASH; break;
      case '@': e = KeyEvent.KEYCODE_AT; break;
      case '+': e = KeyEvent.KEYCODE_PLUS; break;
      case ',': e = KeyEvent.KEYCODE_COMMA; break;
      case '.': e = KeyEvent.KEYCODE_PERIOD; break;
      case '*': e = KeyEvent.KEYCODE_STAR; break;
      case '#': e = KeyEvent.KEYCODE_POUND; break;
      case '(': e = KeyEvent.KEYCODE_NUMPAD_LEFT_PAREN; break;
      case ')': e = KeyEvent.KEYCODE_NUMPAD_RIGHT_PAREN; break;
      case ' ': e = KeyEvent.KEYCODE_SPACE; break;
      default: return k;
    }
    return k.withKeyevent(e);
  }

  /** Remove placeholder keys that haven't been modified into something. */
  private static KeyValue remove_placeholders(KeyValue k)
  {
    switch (k.name)
    {
      case "f11_placeholder":
      case "f12_placeholder": return removed_key;
      default: return k;
    }
  }

  /* Lookup the cache entry for a key. Create it needed. */
  private static HashMap<Pointers.Modifiers, KeyValue> cacheEntry(KeyValue k)
  {
    HashMap<Pointers.Modifiers, KeyValue> ks = _cache.get(k.name);
    if (ks == null)
    {
      ks = new HashMap<Pointers.Modifiers, KeyValue>();
      _cache.put(k.name, ks);
    }
    return ks;
  }

  private static abstract class Map_char
  {
    public abstract char apply(char c);
  }

  private static char map_char_shift(char c)
  {
    switch (c)
    {
      case '↙': return '⇙';
      case '↓': return '⇓';
      case '↘': return '⇘';
      case '←': return '⇐';
      case '→': return '⇒';
      case '↖': return '⇖';
      case '↑': return '⇑';
      case '↗': return '⇗';
      case '└': return '╚';
      case '┴': return '╩';
      case '┘': return '╝';
      case '├': return '╠';
      case '┼': return '╬';
      case '┤': return '╣';
      case '┌': return '╔';
      case '┬': return '╦';
      case '┐': return '╗';
      case '─': return '═';
      case '│': return '║';
      default: return c;
    }
  }

  private static final Map_char map_char_double_aigu =
    new Map_char() {
      public char apply(char c)
      {
        switch (c)
        {
          // Composite characters: a̋ e̋ i̋ m̋ ӳ
          case 'o': return 'ő';
          case 'u': return 'ű';
          case ' ': return '˝';
          default: return c;
        }
      }
    };

  private static final Map_char map_char_ordinal =
    new Map_char() {
      public char apply(char c)
      {
        switch (c)
        {
          case 'a': return 'ª';
          case 'o': return 'º';
          case '1': return 'ª';
          case '2': return 'º';
          case '3': return 'ⁿ';
          case '4': return 'ᵈ';
          case '5': return 'ᵉ';
          case '6': return 'ʳ';
          case '7': return 'ˢ';
          case '8': return 'ᵗ';
          case '9': return 'ʰ';
          case '*': return '°';
          default: return c;
        }
      }
    };

  private static final Map_char map_char_superscript =
    new Map_char() {
      public char apply(char c)
      {
        switch (c)
        {
          case '1': return '¹';
          case '2': return '²';
          case '3': return '³';
          case '4': return '⁴';
          case '5': return '⁵';
          case '6': return '⁶';
          case '7': return '⁷';
          case '8': return '⁸';
          case '9': return '⁹';
          case '0': return '⁰';
          case 'i': return 'ⁱ';
          case '+': return '⁺';
          case '-': return '⁻';
          case '=': return '⁼';
          case '(': return '⁽';
          case ')': return '⁾';
          case 'n': return 'ⁿ';
          default: return c;
        }
      }
    };

  private static final Map_char map_char_subscript =
    new Map_char() {
      public char apply(char c)
      {
        switch (c)
        {
          case '1': return '₁';
          case '2': return '₂';
          case '3': return '₃';
          case '4': return '₄';
          case '5': return '₅';
          case '6': return '₆';
          case '7': return '₇';
          case '8': return '₈';
          case '9': return '₉';
          case '0': return '₀';
          case '+': return '₊';
          case '-': return '₋';
          case '=': return '₌';
          case '(': return '₍';
          case ')': return '₎';
          case 'e': return 'ₑ';
          case 'a': return 'ₐ';
          case 'x': return 'ₓ';
          case 'o': return 'ₒ';
          default: return c;
        }
      }
    };

  private static final Map_char map_char_arrows =
    new Map_char() {
      public char apply(char c)
      {
        switch (c)
        {
          case '1': return '↙';
          case '2': return '↓';
          case '3': return '↘';
          case '4': return '←';
          case '6': return '→';
          case '7': return '↖';
          case '8': return '↑';
          case '9': return '↗';
          default: return c;
        }
      }
    };

  private static final Map_char map_char_box =
    new Map_char() {
      public char apply(char c)
      {
        switch (c)
        {
          case '1': return '└';
          case '2': return '┴';
          case '3': return '┘';
          case '4': return '├';
          case '5': return '┼';
          case '6': return '┤';
          case '7': return '┌';
          case '8': return '┬';
          case '9': return '┐';
          case '0': return '─';
          case '.': return '│';
          default: return c;
        }
      }
    };

  private static final Map_char map_char_slash =
    new Map_char() {
      public char apply(char c)
      {
        switch (c)
        {
          case 'a': return 'ⱥ';
          case 'c': return 'ȼ';
          case 'e': return 'ɇ';
          case 'g': return 'ꞡ';
          case 'l': return 'ł';
          case 'n': return 'ꞥ';
          case 'o': return 'ø';
          case ' ': return '/';
          default: return c;
        }
      }
    };
}
