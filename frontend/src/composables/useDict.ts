import { ref, onMounted } from 'vue'
import { getDictTables, getDictList, createDict, updateDict, deleteDict } from '@/api/dict'
import type { R } from '@/types/api'

/**
 * 字典管理 Composable
 * 提供字典表的增删改查功能
 * @param tableName 字典表名
 */
export function useDict(tableName: string) {
  const loading = ref(false)
  const dictList = ref<Record<string, unknown>[]>([])
  const dictTables = ref<string[]>([])

  /**
   * 获取字典表名列表
   */
  const fetchDictTables = async () => {
    try {
      const res = await getDictTables()
      if (res.code === 200) {
        dictTables.value = res.data || []
      }
    } catch (error) {
      console.error('获取字典表列表失败', error)
    }
  }

  /**
   * 获取字典列表
   */
  const fetchDictList = async () => {
    if (!tableName) return
    loading.value = true
    try {
      const res = await getDictList(tableName)
      if (res.code === 200) {
        dictList.value = res.data || []
      }
    } catch (error) {
      console.error('获取字典列表失败', error)
    } finally {
      loading.value = false
    }
  }

  /**
   * 新增字典
   * @param data 字典数据
   */
  const addDict = async (data: Record<string, unknown>) => {
    try {
      const res = await createDict(tableName, data)
      if (res.code === 200 && res.data) {
        await fetchDictList()
        return true
      }
      return false
    } catch (error) {
      console.error('新增字典失败', error)
      return false
    }
  }

  /**
   * 修改字典
   * @param id 字典ID
   * @param data 字典数据
   */
  const editDict = async (id: number, data: Record<string, unknown>) => {
    try {
      const res = await updateDict(tableName, id, data)
      if (res.code === 200 && res.data) {
        await fetchDictList()
        return true
      }
      return false
    } catch (error) {
      console.error('修改字典失败', error)
      return false
    }
  }

  /**
   * 删除字典
   * @param id 字典ID
   */
  const removeDict = async (id: number) => {
    try {
      const res = await deleteDict(tableName, id)
      if (res.code === 200 && res.data) {
        await fetchDictList()
        return true
      }
      return false
    } catch (error) {
      console.error('删除字典失败', error)
      return false
    }
  }

  // 组件挂载时自动获取字典表列表
  onMounted(() => {
    fetchDictTables()
  })

  return {
    loading,
    dictList,
    dictTables,
    fetchDictTables,
    fetchDictList,
    addDict,
    editDict,
    removeDict
  }
}
