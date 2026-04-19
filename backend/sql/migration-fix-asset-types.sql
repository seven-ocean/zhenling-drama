-- 修复素材类型错误的数据
-- 根据文件名扩展名和mime_type重新判断文件类型

-- 1. 先将所有视频文件类型修正（根据mime_type或文件名）
UPDATE assets 
SET type = 'video'
WHERE deleted = 0 
  AND type != 'video'
  AND (
    mime_type LIKE 'video/%'
    OR filename REGEXP '\.(mp4|mkv|avi|mov|wmv|flv|webm|m4v|3gp|mpeg|mpg)$'
  );

-- 2. 先将所有音频文件类型修正（根据mime_type或文件名）
UPDATE assets 
SET type = 'audio'
WHERE deleted = 0 
  AND type != 'audio'
  AND (
    mime_type LIKE 'audio/%'
    OR filename REGEXP '\.(mp3|wav|ogg|aac|flac|m4a|wma|opus|webm)$'
  );

-- 3. 先将所有图片文件类型修正（根据mime_type或文件名）
UPDATE assets 
SET type = 'image'
WHERE deleted = 0 
  AND type != 'image'
  AND (
    mime_type LIKE 'image/%'
    OR filename REGEXP '\.(jpg|jpeg|png|gif|bmp|webp|svg|ico|tiff)$'
  );

-- 4. 查看修复后的统计
SELECT 
  type,
  COUNT(*) as count
FROM assets 
WHERE deleted = 0 
GROUP BY type;

-- 5. 查看可能被错误分类的文件（用于验证）
SELECT 
  id,
  filename,
  type,
  mime_type,
  file_path
FROM assets 
WHERE deleted = 0 
  AND (
    (filename LIKE '%.mp4%' AND type != 'video')
    OR (filename LIKE '%.mp3%' AND type != 'audio')
    OR (filename LIKE '%.jpg%' AND type != 'image')
  )
ORDER BY type, filename;
