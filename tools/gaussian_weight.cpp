#include <stdio.h>
#include <math.h>
#include "cl_platform.h"
#include <iostream>

static inline int
cvRound(double value)
{
#if ((defined _MSC_VER && defined _M_X64) || (defined __GNUC__ && defined __x86_64__ \
            && defined __SSE2__ && !defined __APPLE__)) && !defined(__CUDACC__)
    __m128d t = _mm_set_sd( value );
    return _mm_cvtsd_si32(t);
#elif defined _MSC_VER && defined _M_IX86
    int t;
    __asm
    {
        fld value;
        fistp t;
    }
    return t;
#elif ((defined _MSC_VER && defined _M_ARM) || defined CV_ICC || \
        defined __GNUC__) && defined HAVE_TEGRA_OPTIMIZATION
    TEGRA_ROUND_DBL(value);
#elif defined CV_ICC || defined __GNUC__
# if CV_VFP
    ARM_ROUND_DBL(value);
# else
    return (int)lrint(value);
# endif
#else
    /* it's ok if round does not comply with IEEE754 standard;
     *        the tests should allow +/-1 difference when the tested functions use round */
    return (int)(value + (value >= 0 ? 0.5 : -0.5));
#endif
}

static void GaussianSmooth2D(/*const Mat &src, Mat &dst,*/double sigma)
{
#if 0
    if(src.channels() != 1)
        return;
#endif //确保sigma为正数
    sigma = sigma > 0 ? sigma : 0;
    //高斯核矩阵的大小为(6*sigma+1)*(6*sigma+1)
    //ksize为奇数
    int ksize = cvRound(sigma * 3) * 2 + 1;

#if 0
//  dst.create(src.size(), src.type());
    if(ksize == 1)
    {
        src.copyTo(dst);
        return;
    }

    dst.create(src.size(), src.type());
#endif

    //计算高斯核矩阵
    double *kernel = new double[ksize*ksize];

    double scale = -0.5/(sigma*sigma);
    const double PI = 3.141592653;
    double cons = -scale/PI;

    double sum = 0;

    for(int i = 0; i < ksize; i++)
    {
        for(int j = 0; j < ksize; j++)
        {
            int x = i-(ksize-1)/2;
            int y = j-(ksize-1)/2;
            kernel[i*ksize + j] = cons * exp(scale * (x*x + y*y));

            sum += kernel[i*ksize+j];
//          cout << " " << kernel[i*ksize + j];
        }
//      cout <<endl;
    }
    //归一化
    for(int i = ksize*ksize-1; i >=0; i--)
    {
        *(kernel+i) /= sum;
    }

    printf("float[%d] weights;/*%d*/\n", ksize*ksize, ksize);
    for(int i = 0; i < ksize*ksize; i++)
    {
        printf("weights[%d] = %0.17lf;\n", i, *(kernel+i));
    }
}

static void GaussianSmooth(/*const Mat &src, Mat &dst,*/double sigma)
{
    sigma = sigma > 0 ? sigma : 0;
    //高斯核矩阵的大小为(6*sigma+1)*(6*sigma+1)
    //ksize为奇数
    int ksize = ceil(sigma * 3) * 2 + 1;
    //计算一维高斯核
    double *kernel = new double[ksize];

    double scale = -0.5/(sigma*sigma);
    const double PI = 3.141592653;
    double cons = 1/sqrt(-scale / PI);

    double sum = 0;
    int kcenter = ksize/2;
    int i = 0, j = 0;
    for(i = 0; i < ksize; i++)
    {
        int x = i - kcenter;
        *(kernel+i) = cons * exp(x * x * scale);//一维高斯函数
        sum += *(kernel+i);
    }
    //归一化,确保高斯权值在[0,1]之间
    for(i = 0; i < ksize; i++)
    {
        *(kernel+i) /= sum;
    }

    printf("float[%d] weights; /*sigma:%0.8lf*/\n", ksize, sigma);
    for(int i = 0; i < ksize; i++)
    {
        printf("weights[%d] = %0.17lf;\n", i, *(kernel+i));
    }
}

static void help() {
    printf("Usage: gaussian <dimension> <sigma>\n"
           "  `dimensio' must be 1 or 2, it means the dimension of gaussian function \n"
           "example:  gaussian 1 0.84089642 \n\n");
}
int main(int argc, char* argv[]) {
    if (argc < 3) {
        help();
        return -1;
    }

    int dimension = 1;
    double sigma;
    sscanf(argv[1], "%d", &dimension);
    sscanf(argv[2], "%lf", &sigma);
    if (dimension == 1) {
        GaussianSmooth(sigma);
    } else if (dimension == 2) {
        GaussianSmooth2D(sigma);
    } else {
        help();
        return -1;
    }

    //printf("sigma is %.8f\n", sigma);
    return 0;
}
