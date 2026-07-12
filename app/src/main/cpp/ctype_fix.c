#define _GNU_SOURCE

#define _ISupper  0x0100
#define _ISlower  0x0200
#define _ISalpha  0x0400
#define _ISdigit  0x0800
#define _ISxdigit 0x1000
#define _ISspace  0x2000
#define _ISprint  0x4000
#define _ISgraph  0x8000
#define _ISblank  0x0001
#define _IScntrl  0x0002
#define _ISpunct  0x0004
#define _ISalnum  0x0008

static unsigned short b_table[384];
static int lower_table[384];
static int upper_table[384];
static const unsigned short *b_ptr;
static const int *lower_ptr;
static const int *upper_ptr;

__attribute__((constructor))
static void ctype_fix_init(void) {
    for (int i = 0; i < 256; i++) {
        unsigned short f = 0;
        if (i < 32 || i == 127) f |= _IScntrl;
        if (i >= 'A' && i <= 'Z') f |= _ISupper | _ISalpha | _ISalnum;
        if (i >= 'a' && i <= 'z') f |= _ISlower | _ISalpha | _ISalnum;
        if (i >= '0' && i <= '9') f |= _ISdigit | _ISxdigit | _ISalnum;
        if ((i >= 'A' && i <= 'F') || (i >= 'a' && i <= 'f')) f |= _ISxdigit;
        if (i == ' ' || i == '\t' || i == '\n' || i == '\v' || i == '\f' || i == '\r') f |= _ISspace;
        if (i == ' ' || i == '\t') f |= _ISblank;
        if (i > 32 && i < 127) f |= _ISgraph;
        if (i >= 32 && i < 127) f |= _ISprint;
        if ((f & _ISprint) && !(f & (_ISalnum | _ISspace))) f |= _ISpunct;
        b_table[128 + i] = f;
        lower_table[128 + i] = (i >= 'A' && i <= 'Z') ? (i + 32) : i;
        upper_table[128 + i] = (i >= 'a' && i <= 'z') ? (i - 32) : i;
    }
    b_ptr = b_table + 128;
    lower_ptr = lower_table + 128;
    upper_ptr = upper_table + 128;
}

const unsigned short **__ctype_b_loc(void) {
    return (const unsigned short **)&b_ptr;
}

const int **__ctype_tolower_loc(void) {
    return (const int **)&lower_ptr;
}

const int **__ctype_toupper_loc(void) {
    return (const int **)&upper_ptr;
}
