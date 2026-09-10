import runpy
import traceback

try:
    print('TAJWID_UI_PATCH_BEGIN', flush=True)
    runpy.run_path('/tmp/quran-tajwid-mushaf-ui-patch.py', run_name='__main__')
    print('TAJWID_UI_PATCH_OK', flush=True)
except BaseException:
    print('TAJWID_UI_PATCH_FAILED', flush=True)
    traceback.print_exc()
    raise
